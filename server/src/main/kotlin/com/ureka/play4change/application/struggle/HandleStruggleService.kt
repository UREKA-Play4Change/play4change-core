package com.ureka.play4change.application.struggle

import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.struggle.AdaptiveTask
import com.ureka.play4change.domain.struggle.ErrorPattern
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.struggle.StruggleSession
import com.ureka.play4change.domain.struggle.StruggleStatus
import com.ureka.play4change.domain.enrollment.TaskShuffleSeed
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.model.GenerationStatus
import com.ureka.play4change.model.ReuseStrategy
import com.ureka.play4change.model.StruggleContext
import com.ureka.play4change.port.TaskGenerationPort
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class HandleStruggleService(
    private val struggleRepository: StruggleRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val topicRepository: TopicRepository,
    private val topicModuleRepository: TopicModuleRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val taskGenerationPort: TaskGenerationPort,
    private val registry: MeterRegistry,
    @Value("\${ai.mistral.timeout-seconds:60}") private val timeoutSeconds: Long
) {

    private val log = LoggerFactory.getLogger(HandleStruggleService::class.java)

    @Async("generationExecutor")
    fun triggerAsync(
        enrollmentId: String,
        assignmentId: String,
        errorPattern: ErrorPattern,
        template: TaskTemplate,
        userId: String
    ) = doTrigger(enrollmentId, assignmentId, errorPattern, template, userId)

    /**
     * Spawns a follow-up struggle session after the learner failed one or more
     * tasks in a previous adaptive session. The same [StruggleSession.originalTaskAssignmentId]
     * is preserved so the chain always traces back to the one main-path task.
     */
    @Async("generationExecutor")
    fun triggerFromPreviousSession(previousSession: StruggleSession, userId: String) {
        val originalAssignment = enrollmentRepository.findAssignmentById(previousSession.originalTaskAssignmentId) ?: run {
            log.error("Original assignment {} not found for follow-up struggle", previousSession.originalTaskAssignmentId)
            return
        }
        val template = taskTemplateRepository.findById(originalAssignment.taskTemplateId) ?: run {
            log.error("Template {} not found for follow-up struggle", originalAssignment.taskTemplateId)
            return
        }
        doTrigger(previousSession.enrollmentId, previousSession.originalTaskAssignmentId, previousSession.errorPattern, template, userId)
    }

    // ---------------------------------------------------------------------------
    // Core generation logic (shared by both trigger paths)
    // ---------------------------------------------------------------------------

    private fun doTrigger(
        enrollmentId: String,
        assignmentId: String,
        errorPattern: ErrorPattern,
        template: TaskTemplate,
        userId: String
    ) {
        var session: StruggleSession? = null
        try {
            val enrollment = enrollmentRepository.findById(enrollmentId) ?: run {
                log.error("Enrollment {} not found for struggle trigger", enrollmentId)
                return
            }
            val topic = topicRepository.findById(enrollment.topicId) ?: run {
                log.error("Topic {} not found for struggle trigger", enrollment.topicId)
                return
            }
            val modules = topicModuleRepository.findByTopicId(enrollment.topicId)
            val module = modules.firstOrNull() ?: run {
                log.error("No module found for topic {} during struggle trigger", enrollment.topicId)
                return
            }

            session = struggleRepository.save(
                StruggleSession(
                    id = UUID.randomUUID().toString(),
                    enrollmentId = enrollmentId,
                    originalTaskAssignmentId = assignmentId,
                    errorPattern = errorPattern,
                    attemptCount = 2,
                    detectedAt = OffsetDateTime.now(),
                    resolvedAt = null,
                    status = StruggleStatus.OPEN,
                    adaptiveTasks = emptyList()
                )
            )

            try {
                val errorPatternTag = when (errorPattern) {
                    ErrorPattern.WRONG_CONCEPT -> "wrong_concept"
                    ErrorPattern.PARTIAL_UNDERSTANDING -> "partial_understanding"
                    ErrorPattern.READING_ERROR -> "reading_error"
                    ErrorPattern.TIME_PRESSURE -> "time_pressure"
                }
                registry.counter("struggle_sessions_total", "error_pattern", errorPatternTag).increment()
                registry.counter(
                    "struggle_sessions_created_total",
                    "topic_id", enrollment.topicId
                ).increment()
            } catch (ex: Exception) {
                log.warn("Metrics registration failed (non-fatal): {}", ex.message)
            }

            val context = StruggleContext(
                userId = userId,
                taskId = template.id,
                moduleId = module.id,
                topicId = enrollment.topicId,
                subjectDomain = template.description.take(2000),
                audienceLevel = com.ureka.play4change.domain.AudienceLevel.valueOf(topic.audienceLevel.name),
                language = topic.language,
                attemptCount = 2,
                errorPattern = mapErrorPattern(errorPattern),
                moduleObjective = module.objective.take(500),
                taskDescription = template.description.take(500)
            )

            val result = runBlocking {
                withTimeoutOrNull(timeoutSeconds * 1_000L) {
                    taskGenerationPort.generateAdaptiveBranch(context)
                }
            }

            if (result == null) {
                log.error("Adaptive branch generation timed out for session {}", session.id)
                struggleRepository.save(session.abandon())
                return
            }

            result.fold(
                ifLeft = { error ->
                    log.error("Adaptive branch generation failed for session {}: {}", session.id, error)
                    struggleRepository.save(session.abandon())
                },
                ifRight = { branch ->
                    val adaptiveTasks = branch.subtasks
                        .filter { it.status == GenerationStatus.SUCCESS }
                        .mapIndexed { idx, task ->
                            val options = task.optionsJson
                                ?.let { parseOptionsJson(it) }
                            val taskId = UUID.randomUUID().toString()
                            val shuffledOrder = if (options != null) {
                                TaskShuffleSeed.shuffleOptions(options.size, userId, taskId, session.id)
                            } else emptyList()
                            AdaptiveTask(
                                id = taskId,
                                struggleSessionId = session.id,
                                // FULL_REUSE copies are user-tracking rows — not canonical.
                                // Set branchId = null so the admin view shows only the original.
                                branchId = if (branch.reuseStrategy == ReuseStrategy.FULL_REUSE) null else branch.branchId,
                                title = task.title,
                                description = task.description,
                                hint = task.hint,
                                pointsReward = task.pointsReward,
                                orderIndex = idx,
                                completedAt = null,
                                isCorrect = null,
                                options = options,
                                correctAnswer = task.correctAnswerIndex,
                                selectedOption = null,
                                optionOrder = shuffledOrder
                            )
                        }

                    val sessionWithTasks = session.copy(adaptiveTasks = adaptiveTasks)
                    struggleRepository.save(sessionWithTasks)
                    log.info(
                        "Struggle session {} ready — {} adaptive task(s) generated",
                        session.id, adaptiveTasks.size
                    )
                }
            )
        } catch (ex: Exception) {
            log.error("Unexpected error during struggle generation for enrollment {}: {}", enrollmentId, ex.message, ex)
            session?.let {
                runCatching { struggleRepository.save(it.abandon()) }
                    .onFailure { e -> log.error("Failed to abandon session {} after error: {}", it.id, e.message) }
            }
        }
    }

    private fun mapErrorPattern(pattern: ErrorPattern): com.ureka.play4change.model.ErrorPattern =
        when (pattern) {
            ErrorPattern.WRONG_CONCEPT -> com.ureka.play4change.model.ErrorPattern.CONCEPTUAL_MISUNDERSTANDING
            ErrorPattern.PARTIAL_UNDERSTANDING -> com.ureka.play4change.model.ErrorPattern.PROCEDURAL_ERROR
            ErrorPattern.READING_ERROR -> com.ureka.play4change.model.ErrorPattern.UNCLEAR_INSTRUCTIONS
            ErrorPattern.TIME_PRESSURE -> com.ureka.play4change.model.ErrorPattern.TIME_PRESSURE
        }

    private fun parseOptionsJson(jsonStr: String): List<String>? = runCatching {
        Json.parseToJsonElement(jsonStr).jsonArray.map { it.jsonPrimitive.content }
    }.getOrNull()
}
