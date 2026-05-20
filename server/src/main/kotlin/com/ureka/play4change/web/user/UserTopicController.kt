package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.EnrollmentUseCase
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
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
    private val enrollmentUseCase: EnrollmentUseCase
) {

    @GetMapping
    fun listAvailable(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<List<UserTopicResponse>> {
        val topics = topicUseCase.listAll(statusFilter = "ACTIVE", page = 0, size = 50).content
        val responses = topics.map { detail ->
            val isEnrolled = enrollmentUseCase.getEnrollment(userId, detail.topic.id)
                .fold(ifLeft = { false }, ifRight = { it.status == EnrollmentStatus.ACTIVE })
            UserTopicResponse.from(detail.topic, isEnrolled)
        }
        return ResponseEntity.ok(responses)
    }
}
