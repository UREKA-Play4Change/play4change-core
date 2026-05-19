package com.ureka.play4change.web.admin

import com.fasterxml.jackson.databind.ObjectMapper
import com.ureka.play4change.application.port.TopicEventPublisher
import com.ureka.play4change.domain.topic.GenerationPhase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

/**
 * SSE adapter that implements [TopicEventPublisher].
 *
 * One [SseEmitter] per topicId is held in memory. The emitter is registered by the
 * HTTP request thread (see [TopicController.streamProgress]) and driven by the
 * @Async generation thread via the [TopicEventPublisher] calls.
 *
 * Thread safety: [ConcurrentHashMap] guards the registry; [SseEmitter.send] is
 * internally synchronized. Failed sends are silently removed so a disconnected
 * client never blocks the generation pipeline.
 */
@Component
class SseTopicEventPublisher(
    private val objectMapper: ObjectMapper
) : TopicEventPublisher {

    private val log = LoggerFactory.getLogger(SseTopicEventPublisher::class.java)
    private val emitters = ConcurrentHashMap<String, SseEmitter>()

    /** Called from [TopicController] when the admin opens the SSE stream. */
    fun register(topicId: String): SseEmitter {
        val emitter = SseEmitter(10 * 60 * 1000L) // 10-minute ceiling
        emitters.put(topicId, emitter)?.complete() // close any stale emitter
        emitter.onCompletion { emitters.remove(topicId, emitter) }
        emitter.onTimeout { emitters.remove(topicId, emitter) }
        emitter.onError { emitters.remove(topicId, emitter) }
        return emitter
    }

    override fun phaseChanged(topicId: String, phase: GenerationPhase, durationMs: Long) =
        send(topicId, "phase-change", mapOf("phase" to phase.name, "durationMs" to durationMs))

    override fun generationProgress(topicId: String, completed: Int, total: Int) =
        send(topicId, "generation-progress", mapOf("completed" to completed, "total" to total))

    override fun completed(topicId: String, totalDurationMs: Long) {
        send(topicId, "complete", mapOf("totalDurationMs" to totalDurationMs))
        emitters.remove(topicId)?.complete()
    }

    override fun failed(topicId: String, reason: String) {
        send(topicId, "failed", mapOf("reason" to reason))
        emitters.remove(topicId)?.complete()
    }

    private fun send(topicId: String, eventName: String, data: Any) {
        val emitter = emitters[topicId] ?: return
        try {
            emitter.send(
                SseEmitter.event()
                    .name(eventName)
                    .data(objectMapper.writeValueAsString(data))
            )
        } catch (ex: Exception) {
            log.debug("SSE send to topic {} dropped — client likely disconnected: {}", topicId, ex.message)
            emitters.remove(topicId, emitter)
        }
    }
}
