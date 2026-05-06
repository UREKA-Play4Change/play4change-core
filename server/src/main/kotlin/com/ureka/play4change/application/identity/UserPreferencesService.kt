package com.ureka.play4change.application.identity

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.UpdatePreferencesCommand
import com.ureka.play4change.application.port.UserPreferences
import com.ureka.play4change.application.port.UserPreferencesUseCase
import com.ureka.play4change.config.LanguageProperties
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.NotFound
import com.ureka.play4change.error.client.UnprocessableEntity
import org.springframework.stereotype.Service
import java.time.ZoneId

private val BCP47_REGEX = Regex("^[a-zA-Z]{2,8}(-[a-zA-Z0-9]{1,8})*\$")

@Service
class UserPreferencesService(
    private val userRepository: UserRepository,
    private val languageProperties: LanguageProperties
) : UserPreferencesUseCase {

    override fun get(userId: String): Either<AppError, UserPreferences> = either {
        val user = ensureNotNull(userRepository.findById(userId)) {
            NotFound.ResourceNotFound("User", userId)
        }
        UserPreferences(language = user.preferredLanguage, timezone = user.timezone)
    }

    override fun update(command: UpdatePreferencesCommand): Either<AppError, UserPreferences> = either {
        val user = ensureNotNull(userRepository.findById(command.userId)) {
            NotFound.ResourceNotFound("User", command.userId)
        }

        val newLanguage = if (command.language != null) {
            val tag = command.language
            // Validate BCP 47 syntax: primary subtag 2-8 letters, optional additional subtags of 1-8 alphanumerics
            ensure(tag.matches(BCP47_REGEX)) {
                BadRequest.InvalidField("language", "invalid BCP 47 tag: $tag")
            }
            // Validate against supported-languages whitelist
            ensure(tag in languageProperties.supportedLanguages) {
                UnprocessableEntity.BusinessRuleViolation(
                    "Unsupported language '$tag'. Supported: ${languageProperties.supportedLanguages.joinToString()}"
                )
            }
            tag
        } else {
            user.preferredLanguage
        }

        val newTimezone = if (command.timezone != null) {
            runCatching { ZoneId.of(command.timezone) }.getOrElse {
                raise(BadRequest.InvalidField("timezone", "invalid ZoneId: ${command.timezone}"))
            }
            command.timezone
        } else {
            user.timezone
        }

        val updated = user.copy(preferredLanguage = newLanguage, timezone = newTimezone)
        userRepository.save(updated)

        UserPreferences(language = newLanguage, timezone = newTimezone)
    }
}
