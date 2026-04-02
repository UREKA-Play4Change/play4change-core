package com.ureka.play4change.infrastructure.persistence.adapter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ureka.play4change.domain.struggle.*
import com.ureka.play4change.infrastructure.persistence.entity.AdaptiveTaskEntity
import com.ureka.play4change.infrastructure.persistence.entity.StruggleSessionEntity
import com.ureka.play4change.infrastructure.persistence.repository.EnrollmentJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.StruggleSessionJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TaskAssignmentJpaRepository
import org.springframework.stereotype.Component

@Component
class StruggleRepositoryAdapter(
    private val jpa: StruggleSessionJpaRepository,
    private val enrollmentJpa: EnrollmentJpaRepository,
    private val assignmentJpa: TaskAssignmentJpaRepository
) : StruggleRepository {

    private val mapper = jacksonObjectMapper()

    override fun findById(id: String): StruggleSession? =
        jpa.findById(id).orElse(null)?.toDomain()

    override fun findOpenByEnrollmentId(enrollmentId: String): StruggleSession? =
        jpa.findByEnrollmentIdAndStatus(enrollmentId, "OPEN")?.toDomain()

    override fun save(session: StruggleSession): StruggleSession {
        val enrollmentEntity = enrollmentJpa.getReferenceById(session.enrollmentId)
        val assignmentEntity = assignmentJpa.getReferenceById(session.originalTaskAssignmentId)

        val entity = StruggleSessionEntity(
            id = session.id,
            enrollment = enrollmentEntity,
            originalTaskAssignment = assignmentEntity,
            errorPattern = session.errorPattern.name,
            attemptCount = session.attemptCount,
            detectedAt = session.detectedAt,
            resolvedAt = session.resolvedAt,
            status = session.status.name
        )

        session.adaptiveTasks.forEachIndexed { _, task ->
            entity.adaptiveTasks.add(
                AdaptiveTaskEntity(
                    id = task.id,
                    struggleSession = entity,
                    title = task.title,
                    description = task.description,
                    hint = task.hint,
                    pointsReward = task.pointsReward,
                    orderIndex = task.orderIndex,
                    completedAt = task.completedAt,
                    isCorrect = task.isCorrect,
                    options = task.options?.let { runCatching { mapper.writeValueAsString(it) }.getOrNull() },
                    correctAnswer = task.correctAnswer,
                    selectedOption = task.selectedOption,
                    optionOrder = task.optionOrder.takeIf { it.isNotEmpty() }
                        ?.let { runCatching { mapper.writeValueAsString(it) }.getOrNull() }
                )
            )
        }

        return jpa.save(entity).toDomain()
    }

    private fun StruggleSessionEntity.toDomain(): StruggleSession = StruggleSession(
        id = id,
        enrollmentId = enrollment.id,
        originalTaskAssignmentId = originalTaskAssignment.id,
        errorPattern = ErrorPattern.valueOf(errorPattern),
        attemptCount = attemptCount,
        detectedAt = detectedAt,
        resolvedAt = resolvedAt,
        status = StruggleStatus.valueOf(status),
        adaptiveTasks = adaptiveTasks.map { it.toDomain() }
    )

    private fun AdaptiveTaskEntity.toDomain(): AdaptiveTask = AdaptiveTask(
        id = id,
        struggleSessionId = struggleSession.id,
        title = title,
        description = description,
        hint = hint,
        pointsReward = pointsReward,
        orderIndex = orderIndex,
        completedAt = completedAt,
        isCorrect = isCorrect,
        options = options?.let { runCatching { mapper.readValue<List<String>>(it) }.getOrNull() },
        correctAnswer = correctAnswer,
        selectedOption = selectedOption,
        optionOrder = optionOrder?.let { runCatching { mapper.readValue<List<Int>>(it) }.getOrNull() } ?: emptyList()
    )
}
