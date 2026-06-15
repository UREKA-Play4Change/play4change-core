package com.ureka.play4change.application.topic

import com.ureka.play4change.application.port.ContentExtractorPort
import com.ureka.play4change.application.port.FileStoragePort
import com.ureka.play4change.application.port.TopicEventPublisher
import com.ureka.play4change.domain.badge.BadgeRepository
import com.ureka.play4change.domain.badge.MicroCompetence
import com.ureka.play4change.domain.topic.ContentSourceType
import com.ureka.play4change.domain.topic.GenerationPhase
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.TopicModule
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.infrastructure.ai.AiContextLimits
import com.ureka.play4change.infrastructure.ai.AiOutputSanitiser
import com.ureka.play4change.infrastructure.ai.OptionsJsonParser
import com.ureka.play4change.model.GenerationRequest
import com.ureka.play4change.model.GenerationStatus
import com.ureka.play4change.port.TaskGenerationPort
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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
    private val batchInstanceGenerationService: BatchInstanceGenerationService,
    private val phaseTransitionService: PhaseTransitionService,
    private val badgeRepository: BadgeRepository,
    private val eventPublisher: TopicEventPublisher,
    private val registry: MeterRegistry,
    @Qualifier("generationCoroutineScope") private val generationScope: CoroutineScope,
    @Value("\${ai.mistral.timeout-seconds:60}") private val timeoutSeconds: Long
) {
    private val log = LoggerFactory.getLogger(TaskGenerationOrchestrator::class.java)

    /**
     * Dispatches generation to the [generationScope] backed by the generation thread pool.
     * Using a coroutine scope instead of `@Async` + `runBlocking` lets the AI call
     * truly suspend rather than blocking the executor thread for the full duration.
     */
    fun generateAsync(topicId: String) {
        generationScope.launch { doGenerate(topicId) }
    }

    @Suppress("LongMethod") // multi-phase pipeline — splitting would obscure the state machine
    private suspend fun doGenerate(topicId: String) {
        val sample = Timer.start(registry)
        val startMs = System.currentTimeMillis()
        try {
            // --- INGESTION phase: fetch/validate content ---
            topicRepository.updateStatus(topicId, TopicStatus.GENERATING)

            val topic = topicRepository.findById(topicId)
                ?: throw IllegalStateException("Topic $topicId not found after status update")

            val rawText = topic.rawExtractedText
                ?: run {
                    val storageKey = contentKeyFor(topicId, topic.contentSourceType)
                    val bytes = fileStoragePort.downloadFile(storageKey)
                    when (topic.contentSourceType) {
                        ContentSourceType.PDF -> contentExtractorPort.extractFromPdf(bytes)
                        ContentSourceType.URL -> bytes.toString(Charsets.UTF_8)
                    }
                }

            // INGESTION → ANALYSIS
            phaseTransitionService.transitionTo(topicId, GenerationPhase.ANALYSIS)

            // --- ANALYSIS phase: clean up previous data and prepare module ---
            topicModuleRepository.findByTopicId(topicId).forEach { module ->
                taskTemplateRepository.markAllSuperseded(module.id)
            }
            topicModuleRepository.deleteByTopicId(topicId)

            val module = topicModuleRepository.save(
                TopicModule(
                    id = UUID.randomUUID().toString(),
                    topicId = topicId,
                    orderIndex = 0,
                    objective = topic.description.take(AiContextLimits.DESCRIPTION_CHARS)
                )
            )

            if (rawText.length > AiContextLimits.CONTENT_CHARS) {
                log.warn("Topic $topicId: content truncated from ${rawText.length} to ${AiContextLimits.CONTENT_CHARS} chars")
            }
            if (topic.description.length > AiContextLimits.DESCRIPTION_CHARS) {
                log.warn("Topic $topicId: module objective truncated from ${topic.description.length} to ${AiContextLimits.DESCRIPTION_CHARS} chars")
            }

            val request = GenerationRequest(
                topicId = topicId,
                moduleId = module.id,
                subjectDomain = rawText.take(AiContextLimits.CONTENT_CHARS),
                audienceLevel = com.ureka.play4change.domain.AudienceLevel.valueOf(topic.audienceLevel.name),
                language = topic.language,
                taskCount = topic.taskCount,
                moduleObjective = topic.description.take(AiContextLimits.DESCRIPTION_CHARS),
                onTaskGenerated = { completed, total ->
                    eventPublisher.generationProgress(topicId, completed, total)
                }
            )

            // ANALYSIS → GENERATION
            phaseTransitionService.transitionTo(topicId, GenerationPhase.GENERATION)

            // --- GENERATION phase: AI call (suspends, doesn't block the thread) ---
            val result = withTimeoutOrNull(timeoutSeconds * 1_000L) {
                taskGenerationPort.generateTasks(request)
            }

            if (result == null) {
                log.error("Task generation timed out ({}s) for topic {}", timeoutSeconds, topicId)
                phaseTransitionService.transitionTo(topicId, GenerationPhase.FAILED)
                topicRepository.updateStatus(topicId, TopicStatus.FAILED)
                eventPublisher.failed(topicId, "AI generation timed out after ${timeoutSeconds}s")
                sample.stop(registry.timer("task.generation.duration", "status", "timeout"))
                return
            }

            result.fold(
                ifLeft = { error ->
                    log.error("Task generation returned error for topic {}: {}", topicId, error)
                    phaseTransitionService.transitionTo(topicId, GenerationPhase.FAILED)
                    topicRepository.updateStatus(topicId, TopicStatus.FAILED)
                    eventPublisher.failed(topicId, "AI generation failed: ${error.javaClass.simpleName}")
                    sample.stop(registry.timer("task.generation.duration", "status", "failed"))
                },
                ifRight = { generationResult ->
                    // GENERATION → INDEXING
                    phaseTransitionService.transitionTo(topicId, GenerationPhase.INDEXING)

                    // --- INDEXING phase: persist templates and instances ---
                    val templates = generationResult.tasks
                        .filter { it.status == GenerationStatus.SUCCESS }
                        .mapIndexed { idx, task ->
                            TaskTemplate(
                                id = UUID.randomUUID().toString(),
                                moduleId = module.id,
                                dayIndex = idx,
                                poolIndex = 0,
                                title = AiOutputSanitiser.sanitise(task.title),
                                description = AiOutputSanitiser.sanitise(task.description),
                                hint = AiOutputSanitiser.sanitise(task.hint),
                                taskType = TaskType.MULTIPLE_CHOICE,
                                pointsReward = task.pointsReward,
                                options = task.optionsJson
                                    ?.let { OptionsJsonParser.parse(it) }
                                    ?.map { AiOutputSanitiser.sanitise(it) },
                                correctAnswer = task.correctAnswerIndex,
                                version = 1,
                                isCurrent = true,
                                supersededBy = null,
                                embedding = task.embedding,
                                language = request.language,
                                createdAt = OffsetDateTime.now()
                            )
                        }

                    taskTemplateRepository.saveAll(templates)
                    batchInstanceGenerationService.generateAndSave(templates)

                    // Upsert MicroCompetence — create on first generation, update name/description on regeneration
                    val existing = badgeRepository.findMicroCompetenceByTopicId(topicId)
                    val microCompetence = if (existing != null) {
                        existing.copy(
                            name = topic.title,
                            description = topic.description.ifBlank { topic.title }
                        )
                    } else {
                        MicroCompetence(
                            id = UUID.randomUUID().toString(),
                            name = topic.title,
                            description = topic.description.ifBlank { topic.title },
                            topicId = topicId
                        )
                    }
                    badgeRepository.saveMicroCompetence(microCompetence)
                    log.info("MicroCompetence upserted for topic {}", topicId)

                    // INDEXING → ACTIVE
                    phaseTransitionService.transitionTo(topicId, GenerationPhase.ACTIVE)
                    topicRepository.updateStatus(topicId, TopicStatus.ACTIVE)
                    val totalMs = System.currentTimeMillis() - startMs
                    log.info(
                        "Topic {} generation complete — {} task(s) created in {}ms",
                        topicId, templates.size, totalMs
                    )
                    eventPublisher.completed(topicId, totalMs)
                    sample.stop(registry.timer("task.generation.duration", "status", "success"))
                }
            )
        } catch (ex: Exception) {
            log.error("Unexpected error during task generation for topic {}: {}", topicId, ex.message, ex)
            phaseTransitionService.transitionTo(topicId, GenerationPhase.FAILED)
            topicRepository.updateStatus(topicId, TopicStatus.FAILED)
            eventPublisher.failed(topicId, "Unexpected error: ${ex.message ?: ex.javaClass.simpleName}")
            sample.stop(registry.timer("task.generation.duration", "status", "failed"))
        }
    }

    private fun contentKeyFor(topicId: String, type: ContentSourceType): String =
        "topics/$topicId/${if (type == ContentSourceType.PDF) "content.pdf" else "content.txt"}"
}
