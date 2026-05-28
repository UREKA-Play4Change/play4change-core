package com.ureka.play4change.application.identity

import arrow.core.Either
import com.ureka.play4change.application.port.UpdateProfileNameCommand
import com.ureka.play4change.application.user.GetUserProfileUseCase
import com.ureka.play4change.application.user.UserProfile
import com.ureka.play4change.domain.identity.AuthProvider
import com.ureka.play4change.domain.identity.User
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.identity.UserRole
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.NotFound
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class UpdateProfileNameServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val getUserProfileUseCase = mockk<GetUserProfileUseCase>()
    private val service = UpdateProfileNameService(userRepository, getUserProfileUseCase)

    private val userId = "user-1"

    private val baseUser = User(
        id = userId,
        email = "user@example.com",
        name = "Old Name",
        avatarUrl = null,
        role = UserRole.USER,
        provider = AuthProvider.MAGIC_LINK,
        providerId = null,
        preferredLanguage = "en",
        audienceLevel = "BEGINNER",
        createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    )

    private val stubProfile = UserProfile(
        userId = userId,
        name = "New Name",
        email = "user@example.com",
        streakDays = 0,
        totalPoints = 0,
        accuracy = 0.0f,
        preferredLanguage = "en"
    )

    @Test
    fun `valid name saves trimmed name and returns updated profile`() {
        every { userRepository.findById(userId) } returns baseUser
        every { userRepository.save(any()) } answers { firstArg() }
        every { getUserProfileUseCase.execute(userId) } returns Either.Right(stubProfile)

        val result = service.execute(UpdateProfileNameCommand(userId, "  New Name  "))

        assertEquals(stubProfile, result.getOrNull())
        verify { userRepository.save(match { it.name == "New Name" }) }
    }

    @Test
    fun `blank name returns BadRequest without touching repository`() {
        val result = service.execute(UpdateProfileNameCommand(userId, "   "))

        assert(result.leftOrNull() is BadRequest.InvalidField)
        verify(exactly = 0) { userRepository.findById(any()) }
    }

    @Test
    fun `single character name returns BadRequest`() {
        every { userRepository.findById(userId) } returns baseUser

        val result = service.execute(UpdateProfileNameCommand(userId, "A"))

        assert(result.leftOrNull() is BadRequest.InvalidField)
    }

    @Test
    fun `name longer than 100 characters returns BadRequest`() {
        every { userRepository.findById(userId) } returns baseUser

        val result = service.execute(UpdateProfileNameCommand(userId, "A".repeat(101)))

        assert(result.leftOrNull() is BadRequest.InvalidField)
    }

    @Test
    fun `name with control character returns BadRequest`() {
        every { userRepository.findById(userId) } returns baseUser

        val result = service.execute(UpdateProfileNameCommand(userId, "Bad\u0001Name"))

        assert(result.leftOrNull() is BadRequest.InvalidField)
    }

    @Test
    fun `unknown user returns NotFound`() {
        every { userRepository.findById(userId) } returns null

        val result = service.execute(UpdateProfileNameCommand(userId, "Valid Name"))

        assert(result.leftOrNull() is NotFound.ResourceNotFound)
    }

    @Test
    fun `name of exactly 2 characters is valid`() {
        every { userRepository.findById(userId) } returns baseUser
        every { userRepository.save(any()) } answers { firstArg() }
        every { getUserProfileUseCase.execute(userId) } returns Either.Right(stubProfile.copy(name = "Jo"))

        val result = service.execute(UpdateProfileNameCommand(userId, "Jo"))

        assert(result.isRight())
        verify { userRepository.save(match { it.name == "Jo" }) }
    }

    @Test
    fun `name of exactly 100 characters is valid`() {
        val hundredChars = "A".repeat(100)
        every { userRepository.findById(userId) } returns baseUser
        every { userRepository.save(any()) } answers { firstArg() }
        every { getUserProfileUseCase.execute(userId) } returns Either.Right(stubProfile.copy(name = hundredChars))

        val result = service.execute(UpdateProfileNameCommand(userId, hundredChars))

        assert(result.isRight())
    }
}
