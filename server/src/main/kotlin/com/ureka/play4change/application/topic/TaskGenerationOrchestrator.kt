package com.ureka.play4change.application.topic

import com.ureka.play4change.application.port.ContentExtractorPort
import com.ureka.play4change.application.port.FileStoragePort
import com.ureka.play4change.domain.topic.ContentSourceType
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.TopicModule
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.model.GenerationRequest
import com.ureka.play4change.model.GenerationStatus
import com.ureka.play4change.port.TaskGenerationPort
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TaskGenerationOrchestrator(
    private val topicRepository: TopicRepository,
    private val topicModuleRepository: TopicModuleRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val fileStoragePort: FileStoragePort,
    private val contentExtractorPort: ContentExtractorPort,
    private val taskGenerationPort: TaskGenerationPort,
    @Value("\${ai.mistral.timeout-seconds:60}") private val timeoutSeconds: Long
) {
    private val log = LoggerFactory.getLogger(TaskGenerationOrchestrator::class.java)

    @Async("generationExecutor")
    fun generateAsync(topicId: String) {
        try {
            topicRepository.updateStatus(topicId, TopicStatus.GENERATING)

            val topic = topicRepository.findById(topicId)
                ?: throw IllegalStateException("Topic $topicId not found after status update")

            // Use cached text or re-fetch from MinIO
            val rawText = topic.rawExtractedText
                ?: run {
                    val storageKey = contentKeyFor(topicId, topic.contentSourceType)
                    val bytes = fileStoragePort.downloadFile(storageKey)
                    when (topic.contentSourceType) {
                        ContentSourceType.PDF -> contentExtractorPort.extractFromPdf(bytes)
                        ContentSourceType.URL -> bytes.toString(Charsets.UTF_8)
                    }
                }

            // Clean up any previous modules + task templates (handles regeneration)
            topicModuleRepository.findByTopicId(topicId).forEach { module ->
                taskTemplateRepository.markAllSuperseded(module.id)
            }
            topicModuleRepository.deleteByTopicId(topicId)

            // Create the single module for this topic
            val module = topicModuleRepository.save(
                TopicModule(
                    id = UUID.randomUUID().toString(),
                    topicId = topicId,
                    orderIndex = 0,
                    objective = topic.description.take(500)
                )
            )

            val request = GenerationRequest(
                courseId = topicId,
                moduleId = module.id,
                subjectDomain = rawText.take(8000),
                audienceLevel = com.ureka.play4change.domain.AudienceLevel.valueOf(topic.audienceLevel.name),
                language = topic.language,
                taskCount = topic.taskCount,
                moduleObjective = topic.description.take(500)
            )

            val result = runBlocking {
                withTimeoutOrNull(timeoutSeconds * 1_000L) {
                    taskGenerationPort.generateTasks(request)
                }
            }

            if (result == null) {
                log.error("Task generation timed out ({}s) for topic {}", timeoutSeconds, topicId)
                topicRepository.updateStatus(topicId, TopicStatus.FAILED)
                return
            }

            result.fold(
                ifLeft = { error ->
                    log.error("Task generation returned error for topic {}: {}", topicId, error)
                    topicRepository.updateStatus(topicId, TopicStatus.FAILED)
                },
                ifRight = { generationResult ->
                    val templates = generationResult.tasks
                        .filter { it.status == GenerationStatus.SUCCESS }
                        .mapIndexed { idx, task ->
                            TaskTemplate(
                                id = UUID.randomUUID().toString(),
                                moduleId = module.id,
                                dayIndex = idx,
                                poolIndex = 0,
                                title = task.title,
                                description = task.description,
                                hint = task.hint,
                                taskType = TaskType.MULTIPLE_CHOICE,
                                pointsReward = task.pointsReward,
                                options = task.optionsJson?.let { parseOptionsJson(it) },
                                correctAnswer = task.correctAnswerIndex,
                                version = 1,
                                isCurrent = true,
                                supersededBy = null,
                                embedding = task.embedding,
                                createdAt = OffsetDateTime.now()
                            )
                        }

                    taskTemplateRepository.saveAll(templates)
                    topicRepository.updateStatus(topicId, TopicStatus.ACTIVE)
                    log.info(
                        "Topic {} generation complete — {} task(s) created",
                        topicId, templates.size
                    )
                }
            )
        } catch (ex: Exception) {
            log.error("Unexpected error during task generation for topic {}: {}", topicId, ex.message, ex)
            topicRepository.updateStatus(topicId, TopicStatus.FAILED)
        }
    }

    private fun contentKeyFor(topicId: String, type: ContentSourceType): String =
        "topics/$topicId/${if (type == ContentSourceType.PDF) "content.pdf" else "content.txt"}"

    private fun parseOptionsJson(json: String): List<String>? = runCatching {
        Json.parseToJsonElement(json).jsonArray.map { it.jsonPrimitive.content }
    }.getOrNull()
}
