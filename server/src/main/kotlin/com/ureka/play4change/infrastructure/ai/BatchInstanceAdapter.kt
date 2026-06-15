package com.ureka.play4change.infrastructure.ai

import com.ureka.play4change.application.port.BatchInstancePort
import com.ureka.play4change.domain.AudienceLevel
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.model.GenerationRequest
import com.ureka.play4change.model.GenerationStatus
import com.ureka.play4change.port.TaskGenerationPort
import kotlinx.coroutines.runBlocking
import com.ureka.play4change.infrastructure.ai.OptionsJsonParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Generates distractor variants for a batch of task templates by delegating to the AI provider.
 *
 * Strategy: for each template, call [TaskGenerationPort.generateTasks] asking for [instancesPerTask]
 * tasks on the same subject.  The returned option sets are used as variant distractor pools.
 * The correct answer is always placed at the same index as the parent template's [TaskTemplate.correctAnswer]
 * (option[0] from the AI result, which holds the correct answer by convention, is swapped into
 * position `correctAnswer`).
 */
@Component
class BatchInstanceAdapter(
    private val taskGenerationPort: TaskGenerationPort
) : BatchInstancePort {

    private val log = LoggerFactory.getLogger(BatchInstanceAdapter::class.java)

    override fun generateInstances(
        templates: List<TaskTemplate>,
        instancesPerTask: Int
    ): Map<String, List<List<String>>> {
        val result = mutableMapOf<String, List<List<String>>>()

        templates.forEach { template ->
            val options = template.options
            if (options.isNullOrEmpty()) {
                log.warn("Template {} has no options — skipping instance generation", template.id)
                return@forEach
            }

            val request = GenerationRequest(
                topicId = "",
                moduleId = template.moduleId,
                subjectDomain = template.description,
                audienceLevel = AudienceLevel.INTERMEDIATE,
                language = template.language,
                taskCount = instancesPerTask,
                moduleObjective = template.description.take(MODULE_OBJECTIVE_LIMIT)
            )

            val aiResult = runBlocking { taskGenerationPort.generateTasks(request) }

            aiResult.fold(
                ifLeft = { error ->
                    log.error("Instance generation failed for template {}: {}", template.id, error)
                },
                ifRight = { generationResult ->
                    val variants = generationResult.tasks
                        .filter { it.status == GenerationStatus.SUCCESS && it.optionsJson != null }
                        .mapNotNull { task ->
                            task.optionsJson
                                ?.let { OptionsJsonParser.parse(it) }
                                ?.map { AiOutputSanitiser.sanitise(it) }
                                ?.let { opts -> placeCorrectAtIndex(opts, template.correctAnswer ?: 0) }
                        }
                        .take(instancesPerTask)
                    result[template.id] = variants
                }
            )
        }

        return result
    }

    /**
     * The AI always returns the correct answer at index 0.
     * Swap it to [correctAnswerIndex] so every instance shares the parent template's answer position.
     */
    private fun placeCorrectAtIndex(opts: List<String>, correctAnswerIndex: Int): List<String> {
        if (correctAnswerIndex == 0 || correctAnswerIndex >= opts.size) return opts
        val mutable = opts.toMutableList()
        val tmp = mutable[0]
        mutable[0] = mutable[correctAnswerIndex]
        mutable[correctAnswerIndex] = tmp
        return mutable
    }


    companion object {
        private const val MODULE_OBJECTIVE_LIMIT = 500
    }
}
