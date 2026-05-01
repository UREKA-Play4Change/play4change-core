package com.ureka.play4change.application.topic

import com.ureka.play4change.application.port.BatchInstancePort
import com.ureka.play4change.domain.topic.TaskInstance
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.domain.topic.TaskTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Generates N shuffled instances (variant distractor sets) for each [TaskTemplate] in a topic.
 *
 * Templates are batched in chunks of [MAX_TEMPLATES_PER_CALL] (≤ 10) to avoid exceeding the
 * AI provider's context window.  The resulting [TaskInstance] records are persisted so that
 * [com.ureka.play4change.application.enrollment.TaskService] can select one per user via
 * `instances[abs(seed) % instances.size]`.
 */
@Service
class BatchInstanceGenerationService(
    private val batchInstancePort: BatchInstancePort,
    private val taskInstanceRepository: TaskInstanceRepository,
    @Value("\${task-generation.instances-per-task:5}") val instancesPerTask: Int
) {

    private val log = LoggerFactory.getLogger(BatchInstanceGenerationService::class.java)

    fun generateAndSave(templates: List<TaskTemplate>) {
        if (templates.isEmpty()) return

        templates.chunked(MAX_TEMPLATES_PER_CALL).forEach { chunk ->
            val variantsByTemplate = batchInstancePort.generateInstances(chunk, instancesPerTask)

            val instances = chunk.flatMap { template ->
                val variants = variantsByTemplate[template.id].orEmpty()
                if (variants.isEmpty()) {
                    log.warn("No variants returned for template {}", template.id)
                }
                variants.mapIndexed { idx, opts ->
                    TaskInstance(
                        id = UUID.randomUUID().toString(),
                        taskTemplateId = template.id,
                        instanceIndex = idx,
                        options = opts,
                        correctAnswer = template.correctAnswer ?: 0
                    )
                }
            }

            if (instances.isNotEmpty()) {
                taskInstanceRepository.saveAll(instances)
                log.info("Saved {} task instance(s) for {} template(s)", instances.size, chunk.size)
            }
        }
    }

    companion object {
        const val MAX_TEMPLATES_PER_CALL = 10
    }
}
