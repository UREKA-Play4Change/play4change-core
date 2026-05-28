package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.UpdateProfileNameCommand
import com.ureka.play4change.application.port.UpdateProfileNameUseCase
import com.ureka.play4change.application.user.GetUserProfileUseCase
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.user.dto.UserProfileResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class UpdateProfileNameRequest(val name: String = "")

@RestController
@RequestMapping("/profile")
class UserProfileController(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateProfileNameUseCase: UpdateProfileNameUseCase
) {

    @GetMapping
    fun getProfile(@AuthenticationPrincipal userId: String): ResponseEntity<UserProfileResponse> =
        getUserProfileUseCase.execute(userId).fold(
            ifLeft = { ResponseEntity.notFound().build() },
            ifRight = { profile -> ResponseEntity.ok(UserProfileResponse.from(profile)) }
        )

    @PatchMapping
    fun updateName(
        @RequestBody request: UpdateProfileNameRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<UserProfileResponse> =
        updateProfileNameUseCase.execute(UpdateProfileNameCommand(userId, request.name)).fold(
            ifLeft = { error -> error.toErrorResponse() },
            ifRight = { profile -> ResponseEntity.ok(UserProfileResponse.from(profile)) }
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
