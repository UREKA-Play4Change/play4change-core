package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.EnrollmentUseCase
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
import com.ureka.play4change.domain.topic.PrerequisiteRepository
import com.ureka.play4change.web.user.dto.UserTopicResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/topics")
class UserTopicController(
    private val topicUseCase: TopicUseCase,
    private val enrollmentUseCase: EnrollmentUseCase,
    private val prerequisiteRepository: PrerequisiteRepository
) {

    @GetMapping
    fun listAvailable(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<List<UserTopicResponse>> {
        val topics = topicUseCase.listAll(statusFilter = "ACTIVE", page = 0, size = 50).content

        val completedTopicIds = enrollmentUseCase.getCompletedTopicIds(userId)

        val responses = topics.map { detail ->
            val topicId = detail.topic.id
            val isEnrolled = enrollmentUseCase.getEnrollment(userId, topicId)
                .fold(ifLeft = { false }, ifRight = { it.status == EnrollmentStatus.ACTIVE })
            val prereqIds = prerequisiteRepository.findPrerequisitesByTopicId(topicId)
            val isLocked = prereqIds.any { it !in completedTopicIds }
            UserTopicResponse.from(detail.topic, isEnrolled, isLocked, prereqIds)
        }
        return ResponseEntity.ok(responses)
    }
}
