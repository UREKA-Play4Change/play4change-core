package com.ureka.play4change.application.admin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.NotFound
import org.springframework.stereotype.Service

@Service
class GetAdminProfileService(
    private val userRepository: UserRepository
) : GetAdminProfileUseCase {
    override fun execute(adminId: String): Either<AppError, AdminProfile> {
        val user = userRepository.findById(adminId)
            ?: return NotFound.ResourceNotFound("User", adminId).left()
        return AdminProfile(
            id = user.id,
            email = user.email,
            name = user.name ?: user.email
        ).right()
    }
}
