package com.ureka.play4change.application.identity

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.UpdateProfileNameCommand
import com.ureka.play4change.application.port.UpdateProfileNameUseCase
import com.ureka.play4change.application.port.GetUserProfileUseCase
import com.ureka.play4change.application.user.UserProfile
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.NotFound
import org.springframework.stereotype.Service

private val CONTROL_CHAR_REGEX = Regex("[\\p{Cntrl}]")
private const val NAME_MIN_LENGTH = 2
private const val NAME_MAX_LENGTH = 100

@Service
class UpdateProfileNameService(
    private val userRepository: UserRepository,
    private val getUserProfileUseCase: GetUserProfileUseCase
) : UpdateProfileNameUseCase {

    override fun execute(command: UpdateProfileNameCommand): Either<AppError, UserProfile> = either {
        val trimmed = command.name.trim()

        ensure(trimmed.isNotBlank()) {
            BadRequest.InvalidField("name", "name must not be blank")
        }
        ensure(trimmed.length in NAME_MIN_LENGTH..NAME_MAX_LENGTH) {
            BadRequest.InvalidField("name", "name must be between $NAME_MIN_LENGTH and $NAME_MAX_LENGTH characters")
        }
        ensure(!trimmed.contains(CONTROL_CHAR_REGEX)) {
            BadRequest.InvalidField("name", "name must not contain control characters")
        }

        val user = ensureNotNull(userRepository.findById(command.userId)) {
            NotFound.ResourceNotFound("User", command.userId)
        }

        userRepository.save(user.copy(name = trimmed))

        getUserProfileUseCase.execute(command.userId).bind()
    }
}
