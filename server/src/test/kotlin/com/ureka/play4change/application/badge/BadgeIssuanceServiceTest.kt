package com.ureka.play4change.application.badge

import com.ureka.play4change.domain.badge.Badge
import com.ureka.play4change.domain.badge.BadgeRepository
import com.ureka.play4change.domain.badge.MicroCompetence
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class BadgeIssuanceServiceTest {

    private val badgeRepository = mockk<BadgeRepository>()
    private val enrollmentRepository = mockk<EnrollmentRepository>()
    private val topicRepository = mockk<TopicRepository>()
    private val service = BadgeIssuanceService(badgeRepository, enrollmentRepository, topicRepository)

    private val userId = "user-1"
    private val topicId = "topic-1"
    private val enrollmentId = "enrollment-1"
    private val competenceId = "competence-1"

    private val microCompetence = MicroCompetence(
        id = competenceId,
        name = "Java Basics",
        description = "Understand core Java concepts",
        topicId = topicId
    )

    private fun correctAssignment(): TaskAssignment = mockk {
        every { isCorrect } returns true
        every { status } returns AssignmentStatus.SUBMITTED
    }

    private fun topicWithTaskCount(n: Int): Topic = mockk {
        every { taskCount } returns n
    }

    @Test
    fun `completing the last task triggers badge issuance`() {
        every { topicRepository.findById(topicId) } returns topicWithTaskCount(2)
        every { enrollmentRepository.findAssignmentsByEnrollmentId(enrollmentId) } returns
            listOf(correctAssignment(), correctAssignment())
        every { badgeRepository.findMicroCompetenceByTopicId(topicId) } returns microCompetence
        every { badgeRepository.findBadgeByUserIdAndMicroCompetenceId(userId, competenceId) } returns null
        every { badgeRepository.saveBadge(any()) } answers { firstArg() }

        service.issueBadge(userId, topicId, enrollmentId)

        verify(exactly = 1) { badgeRepository.saveBadge(any()) }
    }

    @Test
    fun `a second issuance call for the same user and competence is a no-op`() {
        every { topicRepository.findById(topicId) } returns topicWithTaskCount(1)
        every { enrollmentRepository.findAssignmentsByEnrollmentId(enrollmentId) } returns
            listOf(correctAssignment())
        every { badgeRepository.findMicroCompetenceByTopicId(topicId) } returns microCompetence
        every { badgeRepository.findBadgeByUserIdAndMicroCompetenceId(userId, competenceId) } returns
            mockk<Badge>()

        service.issueBadge(userId, topicId, enrollmentId)

        verify(exactly = 0) { badgeRepository.saveBadge(any()) }
    }

    @Test
    fun `completing a non-final task does not issue a badge`() {
        every { topicRepository.findById(topicId) } returns topicWithTaskCount(3)
        every { enrollmentRepository.findAssignmentsByEnrollmentId(enrollmentId) } returns
            listOf(correctAssignment())  // only 1 of 3 tasks correct

        service.issueBadge(userId, topicId, enrollmentId)

        verify(exactly = 0) { badgeRepository.saveBadge(any()) }
        verify(exactly = 0) { badgeRepository.findMicroCompetenceByTopicId(any()) }
    }
}
