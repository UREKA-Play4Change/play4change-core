package com.ureka.play4change.infrastructure.language

import com.ureka.play4change.application.port.LanguageGenerationPort
import com.ureka.play4change.infrastructure.ai.AiOutputSanitiser
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.model.GeneratedTask
import com.ureka.play4change.model.GenerationRequest
import com.ureka.play4change.model.GenerationStatus
import com.ureka.play4change.port.TaskGenerationPort
import kotlinx.coroutines.runBlocking
import com.ureka.play4change.infrastructure.ai.OptionsJsonParser
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class LanguageGenerationAdapter(
    private val topicModuleRepository: TopicModuleRepository,
    private val topicRepository: TopicRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val taskGenerationPort: TaskGenerationPort
) : LanguageGenerationPort {

    private val log = LoggerFactory.getLogger(LanguageGenerationAdapter::class.java)

    @Async("generationExecutor")
    override fun triggerGeneration(moduleId: String, dayIndex: Int, language: String) {
        val module = topicModuleRepository.findById(moduleId) ?: run {
            log.warn("Language generation skipped: module {} not found", moduleId)
            return
        }
        val topic = topicRepository.findById(module.topicId) ?: run {
            log.warn("Language generation skipped: topic {} not found for module {}", module.topicId, moduleId)
            return
        }

        log.info("Generating language variant: moduleId={} dayIndex={} language={}", moduleId, dayIndex, language)

        val request = buildRequest(topic, moduleId, language)
        val result = runBlocking { taskGenerationPort.generateTasks(request) }

        result.fold(
            ifLeft = { error ->
                log.error(
                    "Language generation failed for moduleId={} dayIndex={} language={}: {}",
                    moduleId, dayIndex, language, error
                )
            },
            ifRight = { generationResult ->
                persistIfSuccessful(generationResult.tasks, moduleId, dayIndex, language)
            }
        )
    }

    private fun buildRequest(topic: Topic, moduleId: String, language: String) = GenerationRequest(
        topicId = topic.id,
        moduleId = moduleId,
        subjectDomain = topic.rawExtractedText.orEmpty().take(RAW_TEXT_CHAR_LIMIT),
        audienceLevel = com.ureka.play4change.domain.AudienceLevel.valueOf(topic.audienceLevel.name),
        language = language,
        taskCount = 1,
        moduleObjective = topic.description.take(MODULE_OBJECTIVE_CHAR_LIMIT)
    )

    private fun persistIfSuccessful(
        tasks: List<GeneratedTask>,
        moduleId: String,
        dayIndex: Int,
        language: String
    ) {
        val task = tasks.firstOrNull { it.status == GenerationStatus.SUCCESS }
        if (task == null) {
            log.warn(
                "Language generation returned no successful tasks for moduleId={} dayIndex={} language={}",
                moduleId, dayIndex, language
            )
            return
        }
        val template = TaskTemplate(
            id = UUID.randomUUID().toString(),
            moduleId = moduleId,
            dayIndex = dayIndex,
            poolIndex = 0,
            title = AiOutputSanitiser.sanitise(task.title),
            description = AiOutputSanitiser.sanitise(task.description),
            hint = AiOutputSanitiser.sanitise(task.hint),
            taskType = TaskType.MULTIPLE_CHOICE,
            pointsReward = task.pointsReward,
            options = task.optionsJson?.let { OptionsJsonParser.parse(it) }?.map { AiOutputSanitiser.sanitise(it) },
            correctAnswer = task.correctAnswerIndex,
            version = 1,
            isCurrent = true,
            supersededBy = null,
            embedding = task.embedding,
            language = language,
            createdAt = OffsetDateTime.now()
        )
        taskTemplateRepository.saveAll(listOf(template))
        log.info("Language variant saved: moduleId={} dayIndex={} language={}", moduleId, dayIndex, language)
    }


    companion object {
        private const val RAW_TEXT_CHAR_LIMIT = 8_000
        private const val MODULE_OBJECTIVE_CHAR_LIMIT = 500
    }
}
