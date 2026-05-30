package com.ureka.play4change.web.admin

import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.identity.User
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.identity.UserRole
import com.ureka.play4change.web.admin.dto.PageResponse
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

@RestController
@RequestMapping("/admin/users")
class AdminUserController(
    private val userRepository: UserRepository,
    private val enrollmentRepository: EnrollmentRepository
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
}
