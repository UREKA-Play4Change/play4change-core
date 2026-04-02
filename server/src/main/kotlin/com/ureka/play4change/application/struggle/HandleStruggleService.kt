package com.ureka.play4change.application.struggle

import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.struggle.AdaptiveTask
import com.ureka.play4change.domain.struggle.ErrorPattern
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.struggle.StruggleSession
import com.ureka.play4change.domain.struggle.StruggleStatus
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.model.GenerationStatus
import com.ureka.play4change.model.StruggleContext
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
class HandleStruggleService(
    private val struggleRepository: StruggleRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val topicRepository: TopicRepository,
    private val topicModuleRepository: TopicModuleRepository,
    private val taskGenerationPort: TaskGenerationPort,
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
    ) {
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

            val session = struggleRepository.save(
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

            val context = StruggleContext(
                userId = userId,
                taskId = template.id,
                moduleId = module.id,
                courseId = enrollment.topicId,
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
                            val shuffledOrder = (options?.indices?.toMutableList() ?: mutableListOf())
                                .also { it.shuffle() }
                            AdaptiveTask(
                                id = UUID.randomUUID().toString(),
                                struggleSessionId = session.id,
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
        }
    }

    private fun mapErrorPattern(pattern: ErrorPattern): com.ureka.play4change.model.ErrorPattern =
        when (pattern) {
            ErrorPattern.WRONG_CONCEPT -> com.ureka.play4change.model.ErrorPattern.CONCEPTUAL_MISUNDERSTANDING
            ErrorPattern.PARTIAL_UNDERSTANDING -> com.ureka.play4change.model.ErrorPattern.PROCEDURAL_ERROR
            ErrorPattern.READING_ERROR -> com.ureka.play4change.model.ErrorPattern.UNCLEAR_INSTRUCTIONS
            ErrorPattern.TIME_PRESSURE -> com.ureka.play4change.model.ErrorPattern.UNKNOWN
        }

    private fun parseOptionsJson(jsonStr: String): List<String>? = runCatching {
        Json.parseToJsonElement(jsonStr).jsonArray.map { it.jsonPrimitive.content }
    }.getOrNull()
}
