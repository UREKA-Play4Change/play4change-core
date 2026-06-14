package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.EnrollmentUseCase
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.domain.topic.PrerequisiteRepository
import com.ureka.play4change.web.admin.dto.PageResponse
import com.ureka.play4change.web.user.dto.UserTopicResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<UserTopicResponse>> {
        val pageResult = topicUseCase.listAll(statusFilter = "ACTIVE", page = page, size = size)
        val completedTopicIds = enrollmentUseCase.getCompletedTopicIds(userId)
        val responses = pageResult.content.map { detail ->
            val topicId = detail.topic.id
            val enrollmentStatus = enrollmentUseCase.getEnrollment(userId, topicId)
                .fold(ifLeft = { null }, ifRight = { it.status.name })
            val prereqIds = prerequisiteRepository.findPrerequisitesByTopicId(topicId)
            val isLocked = prereqIds.any { it !in completedTopicIds }
            UserTopicResponse.from(detail.topic, enrollmentStatus, isLocked, prereqIds)
        }
        return ResponseEntity.ok(
            PageResponse(
                content = responses,
                page = pageResult.page,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages
            )
        )
    }
}
