package com.ureka.play4change.application.port

import com.ureka.play4change.domain.topic.GenerationPhase

/**
 * Outbound port for streaming topic generation progress events.
 * Implemented by [com.ureka.play4change.web.admin.SseTopicEventPublisher].
 */
interface TopicEventPublisher {
    fun phaseChanged(topicId: String, phase: GenerationPhase, durationMs: Long)
    fun generationProgress(topicId: String, completed: Int, total: Int)
    fun completed(topicId: String, totalDurationMs: Long)
    fun failed(topicId: String, reason: String)
}
