package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.enrollment.*
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.infrastructure.persistence.entity.EnrollmentEntity
import com.ureka.play4change.infrastructure.persistence.entity.TaskAssignmentEntity
import com.ureka.play4change.infrastructure.persistence.repository.EnrollmentJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TaskAssignmentJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TaskTemplateJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TopicJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TopicModuleJpaRepository
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component

@Component
class EnrollmentRepositoryAdapter(
    private val enrollmentJpa: EnrollmentJpaRepository,
    private val assignmentJpa: TaskAssignmentJpaRepository,
    private val topicJpa: TopicJpaRepository,
    private val moduleJpa: TopicModuleJpaRepository,
    private val templateJpa: TaskTemplateJpaRepository
) : EnrollmentRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun findById(id: String): Enrollment? =
        enrollmentJpa.findById(id).orElse(null)?.toDomain()

    override fun findByUserIdAndTopicId(userId: String, topicId: String): Enrollment? =
        enrollmentJpa.findByUserIdAndTopicId(userId, topicId)?.toDomain()

    override fun findActiveByUserId(userId: String): List<Enrollment> =
        enrollmentJpa.findByUserIdAndStatus(userId, "ACTIVE").map { it.toDomain() }

    override fun findAssignmentById(id: String): TaskAssignment? =
        assignmentJpa.findById(id).orElse(null)?.toDomain()

    override fun findAssignmentByEnrollmentAndTemplate(
        enrollmentId: String,
        taskTemplateId: String
    ): TaskAssignment? =
        assignmentJpa.findByEnrollmentIdAndTaskTemplateId(enrollmentId, taskTemplateId)?.toDomain()

    override fun findAssignmentsByEnrollmentId(enrollmentId: String): List<TaskAssignment> =
        assignmentJpa.findAllByEnrollmentId(enrollmentId).map { it.toDomain() }

    override fun findPendingReviewSubmissionsForTopic(topicId: String, excludeUserId: String): List<TaskAssignment> =
        assignmentJpa.findByTopicAndTypeAndStatusExcludingUser(
            topicId = topicId,
            taskType = "TODO_ACTION",
            status = "PENDING_REVIEW",
            excludeUserId = excludeUserId
        ).map { it.toDomain() }

    override fun save(enrollment: Enrollment): Enrollment {
        val topicEntity = topicJpa.getReferenceById(enrollment.topicId)
        val moduleEntity = moduleJpa.getReferenceById(enrollment.topicModuleId)
        val entity = EnrollmentEntity(
            id = enrollment.id,
            userId = enrollment.userId,
            topic = topicEntity,
            topicModule = moduleEntity,
            enrolledAt = enrollment.enrolledAt,
            status = enrollment.status.name,
            currentDayIndex = enrollment.currentDayIndex,
            totalPointsEarned = enrollment.totalPointsEarned,
            streakDays = enrollment.streakDays,
            lastActivityAt = enrollment.lastActivityAt
        )
        return enrollmentJpa.save(entity).toDomain()
    }

    override fun saveAssignment(assignment: TaskAssignment): TaskAssignment {
        val enrollmentEntity = enrollmentJpa.getReferenceById(assignment.enrollmentId)
        val templateEntity = templateJpa.getReferenceById(assignment.taskTemplateId)
        val optionOrderJson = json.encodeToString(ListSerializer(Int.serializer()), assignment.optionOrder)
        val entity = TaskAssignmentEntity(
            id = assignment.id,
            enrollment = enrollmentEntity,
            userId = assignment.userId,
            taskTemplate = templateEntity,
            taskTemplateVersion = assignment.taskTemplateVersion,
            taskType = assignment.taskType.name,
            assignedAt = assignment.assignedAt,
            dueAt = assignment.dueAt,
            submittedAt = assignment.submittedAt,
            status = assignment.status.name,
            selectedOption = assignment.selectedOption,
            isCorrect = assignment.isCorrect,
            pointsAwarded = assignment.pointsAwarded,
            optionOrder = optionOrderJson,
            wrongAttemptCount = assignment.wrongAttemptCount,
            photoUrl = assignment.photoUrl
        )
        return assignmentJpa.save(entity).toDomain()
    }

    private fun EnrollmentEntity.toDomain(): Enrollment = Enrollment(
        id = id,
        userId = userId,
        topicId = topic.id,
        topicModuleId = topicModule.id,
        enrolledAt = enrolledAt,
        status = EnrollmentStatus.valueOf(status),
        currentDayIndex = currentDayIndex,
        totalPointsEarned = totalPointsEarned,
        streakDays = streakDays,
        lastActivityAt = lastActivityAt
    )

    private fun TaskAssignmentEntity.toDomain(): TaskAssignment {
        val order = optionOrder
            ?.let { json.decodeFromString<List<Int>>(it) }
            ?: emptyList()
        return TaskAssignment(
            id = id,
            enrollmentId = enrollment.id,
            userId = userId,
            taskTemplateId = taskTemplate.id,
            taskTemplateVersion = taskTemplateVersion,
            taskType = TaskType.valueOf(taskType),
            assignedAt = assignedAt,
            dueAt = dueAt,
            submittedAt = submittedAt,
            status = AssignmentStatus.valueOf(status),
            selectedOption = selectedOption,
            isCorrect = isCorrect,
            pointsAwarded = pointsAwarded,
            optionOrder = order,
            wrongAttemptCount = wrongAttemptCount,
            photoUrl = photoUrl
        )
    }
}
