package com.ureka.play4change.web.user

import com.ureka.play4change.application.user.GetUserProfileUseCase
import com.ureka.play4change.web.user.dto.UserProfileResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/profile")
class UserProfileController(private val getUserProfileUseCase: GetUserProfileUseCase) {

    @GetMapping
    fun getProfile(@AuthenticationPrincipal userId: String): ResponseEntity<UserProfileResponse> =
        getUserProfileUseCase.execute(userId).fold(
            ifLeft = { ResponseEntity.notFound().build() },
            ifRight = { profile -> ResponseEntity.ok(UserProfileResponse.from(profile)) }
        )
}
