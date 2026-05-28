package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.application.user.UserProfile
import com.ureka.play4change.error.AppError

data class UpdateProfileNameCommand(
    val userId: String,
    val name: String
)

interface UpdateProfileNameUseCase {
    fun execute(command: UpdateProfileNameCommand): Either<AppError, UserProfile>
}
