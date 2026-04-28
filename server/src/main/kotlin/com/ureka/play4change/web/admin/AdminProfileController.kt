package com.ureka.play4change.web.admin

import com.ureka.play4change.application.admin.GetAdminProfileUseCase
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
class AdminProfileController(private val getAdminProfileUseCase: GetAdminProfileUseCase) {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal adminId: String): ResponseEntity<AdminProfileResponse> {
        return getAdminProfileUseCase.execute(adminId).fold(
            ifLeft = { ResponseEntity.notFound().build() },
            ifRight = { profile ->
                ResponseEntity.ok(
                    AdminProfileResponse(
                        id = profile.id,
                        email = profile.email,
                        name = profile.name ?: profile.email
                    )
                )
            }
        )
    }
}
