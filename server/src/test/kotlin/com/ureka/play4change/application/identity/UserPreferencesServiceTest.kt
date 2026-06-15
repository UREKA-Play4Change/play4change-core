package com.ureka.play4change.application.identity

import com.ureka.play4change.application.port.UpdatePreferencesCommand
import com.ureka.play4change.config.LanguageProperties
import com.ureka.play4change.auth.AuthProvider
import com.ureka.play4change.domain.identity.User
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.identity.UserRole
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.UnprocessableEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class UserPreferencesServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val languageProperties = LanguageProperties().apply {
        supportedLanguages = listOf("en", "pt-PT", "es-ES")
    }

    private val service = UserPreferencesService(userRepository, languageProperties)

    private val userId = "user-1"

    private val baseUser = User(
        id = userId,
        email = "user@example.com",
        name = "Test User",
        avatarUrl = null,
        role = UserRole.USER,
        provider = AuthProvider.MAGIC_LINK,
        providerId = null,
        preferredLanguage = "en",
        audienceLevel = "BEGINNER",
        createdAt = OffsetDateTime.now(ZoneOffset.UTC),
        timezone = null
    )

    @Test
    fun `valid BCP 47 and valid timezone updates both preferences`() {
        every { userRepository.findById(userId) } returns baseUser
        every { userRepository.save(any()) } answers { firstArg() }

        val result = service.update(UpdatePreferencesCommand(userId, language = "pt-PT", timezone = "Europe/Lisbon"))

        val prefs = result.getOrNull()!!
        assertEquals("pt-PT", prefs.language)
        assertEquals("Europe/Lisbon", prefs.timezone)
        verify { userRepository.save(match { it.preferredLanguage == "pt-PT" && it.timezone == "Europe/Lisbon" }) }
    }

    @Test
    fun `invalid BCP 47 tag returns 400 validation error`() {
        every { userRepository.findById(userId) } returns baseUser

        val result = service.update(UpdatePreferencesCommand(userId, language = "not-a-valid-language-tag!!"))

        val error = result.leftOrNull()
        assert(error is BadRequest.InvalidField) { "expected BadRequest.InvalidField but got $error" }
    }

    @Test
    fun `valid tag but unsupported language returns 422`() {
        every { userRepository.findById(userId) } returns baseUser

        val result = service.update(UpdatePreferencesCommand(userId, language = "fr-FR"))

        val error = result.leftOrNull()
        assert(error is UnprocessableEntity.BusinessRuleViolation) {
            "expected UnprocessableEntity.BusinessRuleViolation but got $error"
        }
    }

    @Test
    fun `invalid timezone string returns 400 validation error`() {
        every { userRepository.findById(userId) } returns baseUser

        val result = service.update(UpdatePreferencesCommand(userId, timezone = "NotAZone/Invalid"))

        val error = result.leftOrNull()
        assert(error is BadRequest.InvalidField) { "expected BadRequest.InvalidField but got $error" }
    }

    @Test
    fun `partial update with language only does not overwrite timezone`() {
        val userWithTimezone = baseUser.copy(timezone = "America/New_York")
        every { userRepository.findById(userId) } returns userWithTimezone
        every { userRepository.save(any()) } answers { firstArg() }

        val result = service.update(UpdatePreferencesCommand(userId, language = "pt-PT", timezone = null))

        val prefs = result.getOrNull()!!
        assertEquals("pt-PT", prefs.language)
        assertEquals("America/New_York", prefs.timezone)
        verify { userRepository.save(match { it.timezone == "America/New_York" }) }
    }

    @Test
    fun `GET returns current preferences`() {
        val userWithPrefs = baseUser.copy(preferredLanguage = "es-ES", timezone = "Europe/Madrid")
        every { userRepository.findById(userId) } returns userWithPrefs

        val result = service.get(userId)

        val prefs = result.getOrNull()!!
        assertEquals("es-ES", prefs.language)
        assertEquals("Europe/Madrid", prefs.timezone)
    }

    @Test
    fun `GET returns null timezone when not yet set`() {
        every { userRepository.findById(userId) } returns baseUser

        val result = service.get(userId)

        val prefs = result.getOrNull()!!
        assertEquals("en", prefs.language)
        assertNull(prefs.timezone)
    }
}
