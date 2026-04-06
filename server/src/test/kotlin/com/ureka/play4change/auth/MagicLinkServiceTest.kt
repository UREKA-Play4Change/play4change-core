package com.ureka.play4change.auth

import com.ureka.play4change.auth.application.MagicLinkService
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.MagicLinkToken
import com.ureka.play4change.auth.domain.model.TokenPair
import com.ureka.play4change.auth.domain.model.User
import com.ureka.play4change.auth.port.outbound.EmailPort
import com.ureka.play4change.auth.port.outbound.MagicLinkTokenRepository
import com.ureka.play4change.auth.port.outbound.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime

class MagicLinkServiceTest {

    private val magicLinkTokenRepository: MagicLinkTokenRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val emailPort: EmailPort = mockk()
    private val tokenService: TokenService = mockk()

    private val baseUrl = "http://localhost:8080"

    private val service = MagicLinkService(
        magicLinkTokenRepository = magicLinkTokenRepository,
        userRepository = userRepository,
        emailPort = emailPort,
        tokenService = tokenService,
        baseUrl = baseUrl
    )

    private val existingUser = User(
        id = "user-1",
        email = "demo@example.com",
        name = "Demo User",
        provider = AuthProvider.MAGIC_LINK,
        providerId = null,
        createdAt = OffsetDateTime.now(),
        role = "USER"
    )

    private val tokenPair = TokenPair(
        accessToken = "access.token.jwt",
        refreshToken = "raw-refresh-token",
        accessExpiresInSeconds = 900
    )

    @BeforeEach
    fun setUp() {
        every { magicLinkTokenRepository.save(any()) } answers { firstArg() }
        every { emailPort.sendMagicLink(any(), any()) } returns Unit
    }

    // ── requestMagicLink ─────────────────────────────────────────────────────

    @Test
    fun `requestMagicLink saves a token and calls EmailPort with the correct link`() {
        val tokenSlot = slot<MagicLinkToken>()
        every { magicLinkTokenRepository.save(capture(tokenSlot)) } answers { firstArg() }

        service.requestMagicLink("demo@example.com")

        val savedToken = tokenSlot.captured
        assertEquals("demo@example.com", savedToken.email)
        assertNotNull(savedToken.token)

        verify(exactly = 1) {
            emailPort.sendMagicLink(
                "demo@example.com",
                match { it.startsWith("$baseUrl/auth/verify?token=") }
            )
        }
    }

    @Test
    fun `requestMagicLink normalizes the email to lowercase and trims whitespace`() {
        val tokenSlot = slot<MagicLinkToken>()
        every { magicLinkTokenRepository.save(capture(tokenSlot)) } answers { firstArg() }

        service.requestMagicLink("  DEMO@Example.COM  ")

        assertEquals("demo@example.com", tokenSlot.captured.email)
        verify { emailPort.sendMagicLink("demo@example.com", any()) }
    }

    // ── verifyMagicLink — success paths ──────────────────────────────────────

    @Test
    fun `verifyMagicLink with valid token and existing user returns a TokenPair`() {
        val token = validToken(email = "demo@example.com")
        every { magicLinkTokenRepository.findByToken("abc123") } returns token
        every { magicLinkTokenRepository.markUsed(token.id) } returns Unit
        every { userRepository.findByEmail("demo@example.com") } returns existingUser
        every { tokenService.issue(existingUser.id, existingUser.email, existingUser.role) } returns tokenPair

        val result = service.verifyMagicLink("abc123")

        assertEquals(tokenPair, result)
        verify(exactly = 1) { magicLinkTokenRepository.markUsed(token.id) }
    }

    @Test
    fun `verifyMagicLink creates a new user when email is not yet registered`() {
        val token = validToken(email = "new@example.com")
        val newUser = existingUser.copy(id = "user-new", email = "new@example.com")

        every { magicLinkTokenRepository.findByToken("abc123") } returns token
        every { magicLinkTokenRepository.markUsed(token.id) } returns Unit
        every { userRepository.findByEmail("new@example.com") } returns null
        every { userRepository.save(any()) } returns newUser
        every { tokenService.issue(newUser.id, newUser.email, newUser.role) } returns tokenPair

        val result = service.verifyMagicLink("abc123")

        assertEquals(tokenPair, result)
        verify(exactly = 1) { userRepository.save(any()) }
    }

    // ── verifyMagicLink — failure paths ──────────────────────────────────────

    @Test
    fun `verifyMagicLink throws IllegalArgumentException when token is not found`() {
        every { magicLinkTokenRepository.findByToken("unknown") } returns null

        val ex = assertThrows<IllegalArgumentException> {
            service.verifyMagicLink("unknown")
        }
        assertEquals("Invalid magic link token", ex.message)
    }

    @Test
    fun `verifyMagicLink throws IllegalArgumentException when token is expired`() {
        val expiredToken = MagicLinkToken(
            id = "tok-1",
            token = "abc123",
            email = "demo@example.com",
            expiresAt = OffsetDateTime.now().minusMinutes(1),   // already past
            used = false,
            createdAt = OffsetDateTime.now().minusMinutes(20)
        )
        every { magicLinkTokenRepository.findByToken("abc123") } returns expiredToken

        val ex = assertThrows<IllegalArgumentException> {
            service.verifyMagicLink("abc123")
        }
        assertEquals("Magic link expired or already used", ex.message)
    }

    @Test
    fun `verifyMagicLink throws IllegalArgumentException when token has already been used`() {
        val usedToken = validToken().copy(used = true)
        every { magicLinkTokenRepository.findByToken("abc123") } returns usedToken

        val ex = assertThrows<IllegalArgumentException> {
            service.verifyMagicLink("abc123")
        }
        assertEquals("Magic link expired or already used", ex.message)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun validToken(email: String = "demo@example.com") = MagicLinkToken(
        id = "tok-1",
        token = "abc123",
        email = email,
        expiresAt = OffsetDateTime.now().plusMinutes(14),
        used = false,
        createdAt = OffsetDateTime.now().minusMinutes(1)
    )
}
