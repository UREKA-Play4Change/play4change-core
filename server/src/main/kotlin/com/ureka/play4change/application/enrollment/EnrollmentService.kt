package com.ureka.play4change.application.enrollment

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.EnrollCommand
import com.ureka.play4change.application.port.EnrollmentUseCase
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.Enrollment
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.enrollment.TaskShuffleSeed
import com.ureka.play4change.domain.topic.PrerequisiteRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.NotFound
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class EnrollmentService(
    private val topicRepository: TopicRepository,
    private val topicModuleRepository: TopicModuleRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val prerequisiteRepository: PrerequisiteRepository,
    private val registry: MeterRegistry
) : EnrollmentUseCase {

    private val log = LoggerFactory.getLogger(EnrollmentService::class.java)

    override fun enroll(command: EnrollCommand): Either<AppError, Enrollment> = either {
        val topic = ensureNotNull(topicRepository.findById(command.topicId)) {
            NotFound.ResourceNotFound("Topic", command.topicId)
        }
        ensure(topic.status == TopicStatus.ACTIVE) {
            BadRequest.InvalidField("topicId", "topic is not active")
        }
        ensure(!topic.isExpired()) {
            BadRequest.InvalidField("topicId", "topic has expired")
        }

        // Gate: all prerequisites must be completed before enrollment is allowed
        val prerequisiteIds = prerequisiteRepository.findPrerequisitesByTopicId(command.topicId)
        if (prerequisiteIds.isNotEmpty()) {
            val completedTopicIds = enrollmentRepository
                .findCompletedByUserId(command.userId)
                .map { it.topicId }
                .toSet()
            val missing = prerequisiteIds.filterNot { it in completedTopicIds }
            ensure(missing.isEmpty()) {
                BadRequest.InvalidField("topicId", "prerequisites not completed: $missing")
            }
        }

        val existingEnrollment = enrollmentRepository.findByUserIdAndTopicId(command.userId, command.topicId)
        if (existingEnrollment != null) {
            ensure(existingEnrollment.status == EnrollmentStatus.PAUSED) {
                Conflict.ConcurrentModification
            }
            log.info("User {} re-enrolling in topic {} — reactivating paused enrollment {}", command.userId, command.topicId, existingEnrollment.id)
            return@either enrollmentRepository.save(
                existingEnrollment.copy(status = EnrollmentStatus.ACTIVE, lastActivityAt = OffsetDateTime.now())
            )
        }

        val modules = topicModuleRepository.findByTopicId(command.topicId)
        val module = ensureNotNull(modules.firstOrNull()) {
            NotFound.ResourceNotFound("TopicModule", command.topicId)
        }

        val templates = taskTemplateRepository.findCurrentByModuleId(module.id)
        ensure(templates.isNotEmpty()) {
            BadRequest.InvalidField("topicId", "topic has no tasks yet")
        }

        val now = OffsetDateTime.now()
        val enrollment = enrollmentRepository.save(
            Enrollment(
                id = UUID.randomUUID().toString(),
                userId = command.userId,
                topicId = command.topicId,
                topicModuleId = module.id,
                enrolledAt = now,
                status = EnrollmentStatus.ACTIVE,
                currentDayIndex = 0,
                totalPointsEarned = 0,
                streakDays = 0,
                lastActivityAt = null
            )
        )

        // Create the first task assignment (dayIndex = 0)
        val firstTemplate = templates.minByOrNull { it.dayIndex }!!
        val shuffledOrder = TaskShuffleSeed.shuffleOptions(
            firstTemplate.options?.size ?: 0,
            command.userId,
            firstTemplate.id,
            enrollment.id
        )

        enrollmentRepository.saveAssignment(
            TaskAssignment(
                id = UUID.randomUUID().toString(),
                enrollmentId = enrollment.id,
                userId = command.userId,
                taskTemplateId = firstTemplate.id,
                taskTemplateVersion = firstTemplate.version,
                taskType = firstTemplate.taskType,
                assignedAt = now,
                dueAt = now.plusHours(24),
                submittedAt = null,
                status = AssignmentStatus.PENDING,
                selectedOption = null,
                isCorrect = null,
                pointsAwarded = 0,
                optionOrder = shuffledOrder,
                wrongAttemptCount = 0,
                photoUrl = null
            )
        )

        registry.counter("topic_enrollments_total").increment()
        log.info("User {} enrolled in topic {}", command.userId, command.topicId)
        enrollment
    }

    override fun getEnrollment(userId: String, topicId: String): Either<AppError, Enrollment> = either {
        ensureNotNull(enrollmentRepository.findByUserIdAndTopicId(userId, topicId)) {
            NotFound.ResourceNotFound("Enrollment", "$userId/$topicId")
        }
    }

    override fun getActiveEnrollments(userId: String): List<Enrollment> =
        enrollmentRepository.findActiveByUserId(userId)

    override fun getCompletedTopicIds(userId: String): Set<String> =
        enrollmentRepository.findCompletedByUserId(userId).map { it.topicId }.toSet()

    override fun deactivateEnrollment(userId: String, topicId: String): Either<AppError, Unit> = either {
        val enrollment = ensureNotNull(enrollmentRepository.findByUserIdAndTopicId(userId, topicId)) {
            NotFound.ResourceNotFound("Enrollment", "$userId/$topicId")
        }
        enrollmentRepository.save(enrollment.deactivate())
        log.info("User {} deactivated enrollment in topic {}", userId, topicId)
    }
}
