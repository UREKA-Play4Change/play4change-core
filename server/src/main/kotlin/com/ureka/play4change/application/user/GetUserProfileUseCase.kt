package com.ureka.play4change.application.user

import arrow.core.Either
import com.ureka.play4change.error.AppError

interface GetUserProfileUseCase {
    fun execute(userId: String): Either<AppError, UserProfile>
}
