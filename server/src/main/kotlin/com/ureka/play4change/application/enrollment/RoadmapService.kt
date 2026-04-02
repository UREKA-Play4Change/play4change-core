package com.ureka.play4change.application.enrollment

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.RoadmapNode
import com.ureka.play4change.application.port.RoadmapNodeStatus
import com.ureka.play4change.application.port.RoadmapUseCase
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.struggle.StruggleStatus
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.NotFound
import org.springframework.stereotype.Service

@Service
class RoadmapService(
    private val topicRepository: TopicRepository,
    private val topicModuleRepository: TopicModuleRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val struggleRepository: StruggleRepository
) : RoadmapUseCase {

    override fun getRoadmap(userId: String, topicId: String, timezone: String?): Either<AppError, List<RoadmapNode>> =
        either {
            val enrollment = ensureNotNull(enrollmentRepository.findByUserIdAndTopicId(userId, topicId)) {
                NotFound.ResourceNotFound("Enrollment", "$userId/$topicId")
            }

            val dayIndex = DayIndexCalculator.compute(enrollment.enrolledAt, timezone)

            val modules = topicModuleRepository.findByTopicId(topicId)
            val module = ensureNotNull(modules.firstOrNull()) {
                NotFound.ResourceNotFound("TopicModule", topicId)
            }

            val templates = taskTemplateRepository.findCurrentByModuleId(module.id)
                .sortedBy { it.dayIndex }

            val assignments = enrollmentRepository.findAssignmentsByEnrollmentId(enrollment.id)
            val assignmentsByTemplateId = assignments.associateBy { it.taskTemplateId }

            val openSession = struggleRepository.findOpenByEnrollmentId(enrollment.id)

            val nodes = mutableListOf<RoadmapNode>()

            for (template in templates) {
                val assignment = assignmentsByTemplateId[template.id]
                val nodeStatus = when {
                    template.dayIndex < dayIndex -> when (assignment?.status) {
                        AssignmentStatus.SUBMITTED -> RoadmapNodeStatus.COMPLETED
                        AssignmentStatus.LATE -> RoadmapNodeStatus.LATE
                        AssignmentStatus.SKIPPED -> RoadmapNodeStatus.SKIPPED
                        else -> RoadmapNodeStatus.SKIPPED
                    }
                    template.dayIndex == dayIndex -> RoadmapNodeStatus.PENDING
                    else -> RoadmapNodeStatus.LOCKED
                }
                nodes.add(
                    RoadmapNode(
                        dayIndex = template.dayIndex,
                        title = template.title,
                        status = nodeStatus,
                        isAdaptive = false,
                        assignmentId = assignment?.id,
                        pointsAwarded = assignment?.pointsAwarded
                    )
                )

                // Insert adaptive tasks after today's node if there's an open struggle session
                if (template.dayIndex == dayIndex && openSession != null && openSession.status == StruggleStatus.OPEN) {
                    for (adaptiveTask in openSession.adaptiveTasks.sortedBy { it.orderIndex }) {
                        val adaptiveStatus = when {
                            adaptiveTask.completedAt != null -> RoadmapNodeStatus.ADAPTIVE_COMPLETED
                            else -> RoadmapNodeStatus.ADAPTIVE_PENDING
                        }
                        nodes.add(
                            RoadmapNode(
                                dayIndex = template.dayIndex,
                                title = adaptiveTask.title,
                                status = adaptiveStatus,
                                isAdaptive = true,
                                assignmentId = adaptiveTask.id,
                                pointsAwarded = if (adaptiveTask.completedAt != null && adaptiveTask.isCorrect == true)
                                    adaptiveTask.pointsReward else null
                            )
                        )
                    }
                }
            }

            nodes
        }
}
