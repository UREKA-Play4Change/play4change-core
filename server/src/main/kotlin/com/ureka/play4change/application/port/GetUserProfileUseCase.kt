package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.application.user.UserProfile
import com.ureka.play4change.error.AppError

interface GetUserProfileUseCase {
    fun execute(userId: String): Either<AppError, UserProfile>
}
