package com.ureka.play4change.application.explanation

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.ExplanationUseCase
import com.ureka.play4change.application.port.ResolveExplanationCommand
import com.ureka.play4change.application.port.SendExplanationMessageCommand
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.explanation.ExplanationMessage
import com.ureka.play4change.domain.explanation.ExplanationRepository
import com.ureka.play4change.domain.explanation.ExplanationSession
import com.ureka.play4change.domain.explanation.ExplanationStatus
import com.ureka.play4change.domain.explanation.MessageRole
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.struggle.StruggleStatus
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.Forbidden.ResourceOwnershipViolation
import com.ureka.play4change.error.client.NotFound
import com.ureka.play4change.model.ConversationMessage
import com.ureka.play4change.model.ExplanationContext
import com.ureka.play4change.model.StruggleSummary
import com.ureka.play4change.port.TaskGenerationPort
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ExplanationService(
    private val explanationRepository: ExplanationRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val struggleRepository: StruggleRepository,
    private val topicRepository: TopicRepository,
    private val topicModuleRepository: TopicModuleRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val taskGenerationPort: TaskGenerationPort,
    @Value("\${ai.mistral.timeout-seconds:60}") private val timeoutSeconds: Long
) : ExplanationUseCase {

    private val log = LoggerFactory.getLogger(ExplanationService::class.java)

    /**
     * Creates an ExplanationSession and asynchronously generates the AI explanation.
     * Called from AdaptiveTaskService when the learner exhausts all struggle depth levels.
     * If AI generation fails the session is set to RESOLVED and the original assignment
     * is reset to PENDING so the learner is never stuck.
     */
    @Async("generationExecutor")
    fun triggerAsync(
        enrollmentId: String,
        originalTaskAssignmentId: String,
        errorPattern: String,
        userId: String
    ) {
        val session = explanationRepository.save(
            ExplanationSession(
                id = UUID.randomUUID().toString(),
                enrollmentId = enrollmentId,
                originalTaskAssignmentId = originalTaskAssignmentId,
                errorPattern = errorPattern,
                explanationText = null,
                status = ExplanationStatus.GENERATING,
                generatedAt = OffsetDateTime.now(),
                resolvedAt = null,
                messages = emptyList()
            )
        )

        try {
            val context = buildContext(enrollmentId, originalTaskAssignmentId, errorPattern, userId) ?: run {
                log.error("Could not build ExplanationContext for enrollment {} — aborting explanation generation", enrollmentId)
                fallbackResetAssignment(session)
                return
            }

            val explanationText = runBlocking {
                withTimeoutOrNull(timeoutSeconds * 1_000L) {
                    taskGenerationPort.generateExplanation(context)
                }
            }

            if (explanationText == null) {
                log.error("Explanation generation timed out for session {}", session.id)
                fallbackResetAssignment(session)
                return
            }

            explanationText.fold(
                ifLeft = { error ->
                    log.error("Explanation generation failed for session {}: {}", session.id, error)
                    fallbackResetAssignment(session)
                },
                ifRight = { text ->
                    explanationRepository.save(session.activate(text))
                    log.info("Explanation session {} ready for enrollment {}", session.id, enrollmentId)
                }
            )
        } catch (ex: Exception) {
            log.error("Unexpected error during explanation generation for enrollment {}: {}", enrollmentId, ex.message, ex)
            runCatching { fallbackResetAssignment(session) }
                .onFailure { e -> log.error("Failed fallback reset for session {}: {}", session.id, e.message) }
        }
    }

    override fun getSession(userId: String, sessionId: String): Either<AppError, ExplanationSession> = either {
        val session = ensureNotNull(explanationRepository.findById(sessionId)) {
            NotFound.ResourceNotFound("ExplanationSession", sessionId)
        }
        val enrollment = ensureNotNull(enrollmentRepository.findById(session.enrollmentId)) {
            NotFound.ResourceNotFound("Enrollment", session.enrollmentId)
        }
        ensure(enrollment.userId == userId) {
            ResourceOwnershipViolation("ExplanationSession")
        }
        val messages = explanationRepository.findMessagesBySessionId(sessionId)
        session.copy(messages = messages)
    }

    override fun sendMessage(command: SendExplanationMessageCommand): Either<AppError, ExplanationMessage> = either {
        ensure(command.content.isNotBlank()) {
            BadRequest.InvalidField("content", "must not be blank")
        }
        val session = ensureNotNull(explanationRepository.findById(command.sessionId)) {
            NotFound.ResourceNotFound("ExplanationSession", command.sessionId)
        }
        val enrollment = ensureNotNull(enrollmentRepository.findById(session.enrollmentId)) {
            NotFound.ResourceNotFound("Enrollment", session.enrollmentId)
        }
        ensure(enrollment.userId == command.userId) {
            ResourceOwnershipViolation("ExplanationSession")
        }
        ensure(session.status == ExplanationStatus.ACTIVE) {
            Conflict.ConcurrentModification
        }

        val userMsg = explanationRepository.saveMessage(
            ExplanationMessage(
                id = UUID.randomUUID().toString(),
                sessionId = command.sessionId,
                role = MessageRole.USER,
                content = command.content,
                sentAt = OffsetDateTime.now()
            )
        )

        // Build conversation history for the AI
        val history = explanationRepository.findMessagesBySessionId(command.sessionId)
            .filter { it.id != userMsg.id }
            .map { ConversationMessage(role = it.role.name, content = it.content) }

        val originalAssignment = enrollmentRepository.findAssignmentById(session.originalTaskAssignmentId)
        val context = buildContextFromSession(session, enrollment.userId)
            ?: run {
                // Context build failed — return a fallback message rather than a 500
                return@either explanationRepository.saveMessage(
                    ExplanationMessage(
                        id = UUID.randomUUID().toString(),
                        sessionId = command.sessionId,
                        role = MessageRole.AI,
                        content = "I'm sorry, I encountered a technical issue. Please try again.",
                        sentAt = OffsetDateTime.now()
                    )
                )
            }

        val replyResult = runBlocking {
            withTimeoutOrNull(timeoutSeconds * 1_000L) {
                taskGenerationPort.generateExplanationReply(context, history, command.content)
            }
        }

        val replyText = when {
            replyResult == null -> "I'm sorry, it's taking me too long to respond. Please try again."
            else -> replyResult.fold(
                ifLeft = { "I'm sorry, I encountered a technical issue. Please try again." },
                ifRight = { it }
            )
        }

        explanationRepository.saveMessage(
            ExplanationMessage(
                id = UUID.randomUUID().toString(),
                sessionId = command.sessionId,
                role = MessageRole.AI,
                content = replyText,
                sentAt = OffsetDateTime.now()
            )
        )
    }

    override fun resolve(command: ResolveExplanationCommand): Either<AppError, Unit> = either {
        val session = ensureNotNull(explanationRepository.findById(command.sessionId)) {
            NotFound.ResourceNotFound("ExplanationSession", command.sessionId)
        }
        val enrollment = ensureNotNull(enrollmentRepository.findById(session.enrollmentId)) {
            NotFound.ResourceNotFound("Enrollment", session.enrollmentId)
        }
        ensure(enrollment.userId == command.userId) {
            ResourceOwnershipViolation("ExplanationSession")
        }
        ensure(session.status != ExplanationStatus.RESOLVED) {
            Conflict.ConcurrentModification
        }

        explanationRepository.save(session.resolve())

        // Reset original assignment so learner can retry the main-path task
        val originalAssignment = enrollmentRepository.findAssignmentById(session.originalTaskAssignmentId)
        if (originalAssignment != null) {
            enrollmentRepository.saveAssignment(
                originalAssignment.copy(
                    status = AssignmentStatus.PENDING,
                    submittedAt = null,
                    selectedOption = null,
                    isCorrect = null,
                    pointsAwarded = 0
                )
            )
        }
        log.info("Explanation session {} resolved — original assignment {} reset to PENDING", session.id, session.originalTaskAssignmentId)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** If AI generation fails, skip explanation mode and reset the assignment so the learner can proceed. */
    private fun fallbackResetAssignment(session: ExplanationSession) {
        explanationRepository.save(session.resolve())
        val assignment = enrollmentRepository.findAssignmentById(session.originalTaskAssignmentId)
        if (assignment != null) {
            enrollmentRepository.saveAssignment(
                assignment.copy(
                    status = AssignmentStatus.PENDING,
                    submittedAt = null,
                    selectedOption = null,
                    isCorrect = null,
                    pointsAwarded = 0
                )
            )
        }
        log.warn("Explanation session {} failed generation — assignment {} reset to PENDING as fallback", session.id, session.originalTaskAssignmentId)
    }

    private fun buildContext(
        enrollmentId: String,
        assignmentId: String,
        errorPattern: String,
        userId: String
    ): ExplanationContext? {
        val enrollment = enrollmentRepository.findById(enrollmentId) ?: return null
        val topic = topicRepository.findById(enrollment.topicId) ?: return null
        val module = topicModuleRepository.findByTopicId(enrollment.topicId).firstOrNull() ?: return null
        val assignment = enrollmentRepository.findAssignmentById(assignmentId) ?: return null
        val template = taskTemplateRepository.findById(assignment.taskTemplateId) ?: return null

        val allSessions = struggleRepository.findAllByEnrollmentId(enrollmentId)
            .filter { it.originalTaskAssignmentId == assignmentId && it.status != StruggleStatus.ABANDONED }
            .sortedBy { it.detectedAt }

        val history = allSessions.mapIndexed { idx, session ->
            StruggleSummary(
                depth = idx + 1,
                errorPattern = mapErrorPattern(session.errorPattern),
                taskTitles = session.adaptiveTasks.map { it.title }
            )
        }

        return ExplanationContext(
            userId = userId,
            taskId = template.id,
            topicId = enrollment.topicId,
            taskDescription = template.description.take(500),
            moduleObjective = module.objective.take(500),
            subjectDomain = template.description.take(2000),
            audienceLevel = com.ureka.play4change.domain.AudienceLevel.valueOf(topic.audienceLevel.name),
            language = topic.language,
            errorPattern = mapErrorPattern(errorPattern),
            struggleHistory = history
        )
    }

    private fun buildContextFromSession(session: ExplanationSession, userId: String): ExplanationContext? =
        buildContext(session.enrollmentId, session.originalTaskAssignmentId, session.errorPattern, userId)

    private fun mapErrorPattern(pattern: String): com.ureka.play4change.model.ErrorPattern =
        when (pattern) {
            "WRONG_CONCEPT"         -> com.ureka.play4change.model.ErrorPattern.CONCEPTUAL_MISUNDERSTANDING
            "PARTIAL_UNDERSTANDING" -> com.ureka.play4change.model.ErrorPattern.PROCEDURAL_ERROR
            "READING_ERROR"         -> com.ureka.play4change.model.ErrorPattern.UNCLEAR_INSTRUCTIONS
            "TIME_PRESSURE"         -> com.ureka.play4change.model.ErrorPattern.TIME_PRESSURE
            else                    -> com.ureka.play4change.model.ErrorPattern.UNKNOWN
        }
}
