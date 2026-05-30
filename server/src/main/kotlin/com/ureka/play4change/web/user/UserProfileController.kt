package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.UpdateProfileNameCommand
import com.ureka.play4change.application.port.UpdateProfileNameUseCase
import com.ureka.play4change.application.user.GetUserProfileUseCase
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.user.dto.UserProfileResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class UpdateProfileNameRequest(
    @field:NotBlank @field:Size(min = 2, max = 100) val name: String = ""
)

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
        @Valid @RequestBody request: UpdateProfileNameRequest,
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
