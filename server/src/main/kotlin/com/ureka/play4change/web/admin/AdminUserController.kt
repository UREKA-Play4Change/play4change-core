package com.ureka.play4change.web.admin

import com.ureka.play4change.application.port.BadgeQueryUseCase
import com.ureka.play4change.application.port.RoadmapUseCase
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.identity.User
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.identity.UserRole
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.admin.dto.PageResponse
import com.ureka.play4change.web.user.dto.RoadmapNodeResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

data class UserAdminResponse(
    val id: String,
    val email: String,
    val name: String?,
    val role: String,
    val createdAt: OffsetDateTime,
    val enrollmentCount: Long
) {
    companion object {
        fun from(user: User, enrollmentCount: Long) = UserAdminResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role.name,
            createdAt = user.createdAt,
            enrollmentCount = enrollmentCount
        )
    }
}

data class AdminUserDetailResponse(
    val id: String,
    val email: String,
    val name: String?,
    val role: String,
    val createdAt: OffsetDateTime,
    val enrollmentCount: Int,
    val totalPoints: Int,
    val streakDays: Int
)

data class AdminEnrollmentResponse(
    val enrollmentId: String,
    val topicId: String,
    val topicTitle: String,
    val status: String,
    val currentDayIndex: Int,
    val totalDays: Int,
    val totalPointsEarned: Int,
    val streakDays: Int,
    val enrolledAt: OffsetDateTime
)

data class AdminUserBadgeResponse(
    val microCompetenceName: String,
    val description: String,
    val topicTitle: String,
    val earnedAt: OffsetDateTime
)

@RestController
@RequestMapping("/admin/users")
class AdminUserController(
    private val userRepository: UserRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val topicRepository: TopicRepository,
    private val badgeQueryUseCase: BadgeQueryUseCase,
    private val roadmapUseCase: RoadmapUseCase
) {

    @GetMapping
    fun listUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<UserAdminResponse>> {
        val result = userRepository.findAll(page, size)
        val responses = result.content.map { user ->
            val count = enrollmentRepository.countByUserId(user.id)
            UserAdminResponse.from(user, count)
        }
        return ResponseEntity.ok(
            PageResponse(
                content = responses,
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages
            )
        )
    }

    @PostMapping("/{userId}/promote")
    fun promoteUser(@PathVariable userId: String): ResponseEntity<UserAdminResponse> {
        val user = userRepository.findById(userId)
            ?: return ResponseEntity.notFound().build()
        if (user.role == UserRole.ADMIN) {
            return ResponseEntity.badRequest().build()
        }
        val promoted = user.copy(role = UserRole.ADMIN)
        val saved = userRepository.save(promoted)
        val count = enrollmentRepository.countByUserId(saved.id)
        return ResponseEntity.ok(UserAdminResponse.from(saved, count))
    }

    @GetMapping("/{userId}")
    fun getUserDetail(@PathVariable userId: String): ResponseEntity<AdminUserDetailResponse> {
        val user = userRepository.findById(userId)
            ?: return ResponseEntity.notFound().build()
        val enrollments = enrollmentRepository.findAllByUserId(userId)
        val totalPoints = enrollments.sumOf { it.totalPointsEarned }
        val streakDays = enrollments.maxOfOrNull { it.streakDays } ?: 0
        return ResponseEntity.ok(
            AdminUserDetailResponse(
                id = user.id,
                email = user.email,
                name = user.name,
                role = user.role.name,
                createdAt = user.createdAt,
                enrollmentCount = enrollments.size,
                totalPoints = totalPoints,
                streakDays = streakDays
            )
        )
    }

    @GetMapping("/{userId}/enrollments")
    fun getUserEnrollments(@PathVariable userId: String): ResponseEntity<List<AdminEnrollmentResponse>> {
        val enrollments = enrollmentRepository.findAllByUserId(userId)
        val responses = enrollments.sortedByDescending { it.enrolledAt }.map { enrollment ->
            val topic = topicRepository.findById(enrollment.topicId)
            AdminEnrollmentResponse(
                enrollmentId = enrollment.id,
                topicId = enrollment.topicId,
                topicTitle = topic?.title ?: enrollment.topicId,
                status = enrollment.status.name,
                currentDayIndex = enrollment.currentDayIndex,
                totalDays = topic?.taskCount ?: 0,
                totalPointsEarned = enrollment.totalPointsEarned,
                streakDays = enrollment.streakDays,
                enrolledAt = enrollment.enrolledAt
            )
        }
        return ResponseEntity.ok(responses)
    }

    @GetMapping("/{userId}/badges")
    fun getUserBadges(@PathVariable userId: String): ResponseEntity<List<AdminUserBadgeResponse>> {
        val badges = badgeQueryUseCase.getUserBadges(userId)
        return ResponseEntity.ok(
            badges.map { b ->
                AdminUserBadgeResponse(
                    microCompetenceName = b.microCompetenceName,
                    description = b.description,
                    topicTitle = b.topicTitle,
                    earnedAt = b.earnedAt
                )
            }
        )
    }

    @GetMapping("/{userId}/enrollments/{enrollmentId}/roadmap")
    fun getUserEnrollmentRoadmap(
        @PathVariable userId: String,
        @PathVariable enrollmentId: String
    ): ResponseEntity<List<RoadmapNodeResponse>> {
        val enrollment = enrollmentRepository.findById(enrollmentId)
            ?: return ResponseEntity.notFound().build()
        if (enrollment.userId != userId) return ResponseEntity.notFound().build()
        return roadmapUseCase.getRoadmap(userId, enrollment.topicId, null).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(it.map(RoadmapNodeResponse::from)) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
