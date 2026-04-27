package com.ureka.play4change.web.admin

import com.ureka.play4change.auth.port.outbound.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class AdminProfileResponse(
    val id: String,
    val email: String,
    val name: String
)

@RestController
@RequestMapping("/admin")
class AdminProfileController(private val userRepository: UserRepository) {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal adminId: String): ResponseEntity<AdminProfileResponse> {
        val user = userRepository.findById(adminId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            AdminProfileResponse(
                id = user.id,
                email = user.email,
                name = user.name ?: user.email
            )
        )
    }
}
