package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.explanation.ExplanationMessage
import com.ureka.play4change.domain.explanation.ExplanationRepository
import com.ureka.play4change.domain.explanation.ExplanationSession
import com.ureka.play4change.domain.explanation.ExplanationStatus
import com.ureka.play4change.domain.explanation.MessageRole
import com.ureka.play4change.infrastructure.persistence.entity.ExplanationMessageEntity
import com.ureka.play4change.infrastructure.persistence.entity.ExplanationSessionEntity
import com.ureka.play4change.infrastructure.persistence.repository.EnrollmentJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.ExplanationMessageJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.ExplanationSessionJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TaskAssignmentJpaRepository
import org.springframework.stereotype.Component

@Component
class ExplanationRepositoryAdapter(
    private val jpa: ExplanationSessionJpaRepository,
    private val messageJpa: ExplanationMessageJpaRepository,
    private val enrollmentJpa: EnrollmentJpaRepository,
    private val assignmentJpa: TaskAssignmentJpaRepository
) : ExplanationRepository {

    override fun findById(id: String): ExplanationSession? =
        jpa.findById(id).orElse(null)?.toDomain()

    override fun findActiveByEnrollmentId(enrollmentId: String): ExplanationSession? =
        jpa.findActiveByEnrollmentId(enrollmentId)?.toDomain()

    override fun findAllByEnrollmentId(enrollmentId: String): List<ExplanationSession> =
        jpa.findByEnrollmentIdOrderByGeneratedAtAsc(enrollmentId).map { it.toDomain() }

    override fun save(session: ExplanationSession): ExplanationSession {
        val enrollmentEntity = enrollmentJpa.getReferenceById(session.enrollmentId)
        val assignmentEntity = assignmentJpa.getReferenceById(session.originalTaskAssignmentId)
        val entity = ExplanationSessionEntity(
            id = session.id,
            enrollment = enrollmentEntity,
            originalTaskAssignment = assignmentEntity,
            errorPattern = session.errorPattern,
            explanationText = session.explanationText,
            status = session.status.name,
            generatedAt = session.generatedAt,
            resolvedAt = session.resolvedAt
        )
        return jpa.save(entity).toDomain()
    }

    override fun saveMessage(message: ExplanationMessage): ExplanationMessage {
        val sessionEntity = jpa.getReferenceById(message.sessionId)
        val entity = ExplanationMessageEntity(
            id = message.id,
            session = sessionEntity,
            role = message.role.name,
            content = message.content,
            sentAt = message.sentAt
        )
        return messageJpa.save(entity).toDomain()
    }

    override fun findMessagesBySessionId(sessionId: String): List<ExplanationMessage> =
        messageJpa.findBySessionIdOrderBySentAtAsc(sessionId).map { it.toDomain() }

    private fun ExplanationSessionEntity.toDomain() = ExplanationSession(
        id = id,
        enrollmentId = enrollment.id,
        originalTaskAssignmentId = originalTaskAssignment.id,
        errorPattern = errorPattern,
        explanationText = explanationText,
        status = ExplanationStatus.valueOf(status),
        generatedAt = generatedAt,
        resolvedAt = resolvedAt,
        messages = emptyList() // messages loaded separately to avoid N+1
    )

    private fun ExplanationMessageEntity.toDomain() = ExplanationMessage(
        id = id,
        sessionId = session.id,
        role = MessageRole.valueOf(role),
        content = content,
        sentAt = sentAt
    )
}
