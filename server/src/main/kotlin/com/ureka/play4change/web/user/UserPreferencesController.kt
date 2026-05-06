package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.UpdatePreferencesCommand
import com.ureka.play4change.application.port.UserPreferences
import com.ureka.play4change.application.port.UserPreferencesUseCase
import com.ureka.play4change.error.AppError
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class UpdatePreferencesRequest(
    val language: String? = null,
    val timezone: String? = null
)

@RestController
@RequestMapping("/profile/preferences")
class UserPreferencesController(private val userPreferencesUseCase: UserPreferencesUseCase) {

    @GetMapping
    fun get(@AuthenticationPrincipal userId: String): ResponseEntity<UserPreferences> =
        userPreferencesUseCase.get(userId).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(it) }
        )

    @PutMapping
    fun update(
        @RequestBody request: UpdatePreferencesRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<UserPreferences> =
        userPreferencesUseCase.update(
            UpdatePreferencesCommand(
                userId = userId,
                language = request.language,
                timezone = request.timezone
            )
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(it) }
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
