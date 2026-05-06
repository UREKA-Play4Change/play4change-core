package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.error.AppError

data class UpdatePreferencesCommand(
    val userId: String,
    val language: String? = null,
    val timezone: String? = null
)

data class UserPreferences(
    val language: String,
    val timezone: String?
)

interface UserPreferencesUseCase {
    fun update(command: UpdatePreferencesCommand): Either<AppError, UserPreferences>
    fun get(userId: String): Either<AppError, UserPreferences>
}
