package com.ureka.play4change.application.admin

import arrow.core.Either
import com.ureka.play4change.error.AppError

interface GetAdminProfileUseCase {
    fun execute(adminId: String): Either<AppError, AdminProfile>
}
