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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.codec.Hex
import java.security.MessageDigest
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
        val emailUrlSlot = slot<String>()
        every { magicLinkTokenRepository.save(capture(tokenSlot)) } answers { firstArg() }
        every { emailPort.sendMagicLink(any(), capture(emailUrlSlot)) } returns Unit

        service.requestMagicLink("demo@example.com")

        val savedToken = tokenSlot.captured
        assertEquals("demo@example.com", savedToken.email)
        assertTrue(emailUrlSlot.captured.startsWith("$baseUrl/auth/verify?token="))

        // The DB must store the SHA-256 hash, never the raw token that was emailed
        val rawTokenInEmail = emailUrlSlot.captured.substringAfter("?token=")
        assertEquals(sha256(rawTokenInEmail), savedToken.token, "DB column must store SHA-256 hash, not raw token")
        assertNotEquals(rawTokenInEmail, savedToken.token, "Raw token must NOT be stored in DB")
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
        val rawToken = "abc123"
        every { magicLinkTokenRepository.claimToken(sha256(rawToken)) } returns "demo@example.com"
        every { userRepository.findByEmail("demo@example.com") } returns existingUser
        every { tokenService.issue(existingUser.id, existingUser.email, existingUser.role) } returns tokenPair

        val result = service.verifyMagicLink(rawToken)

        assertEquals(tokenPair, result)
    }

    @Test
    fun `verifyMagicLink creates a new user when email is not yet registered`() {
        val rawToken = "abc123"
        val newUser = existingUser.copy(id = "user-new", email = "new@example.com")

        every { magicLinkTokenRepository.claimToken(sha256(rawToken)) } returns "new@example.com"
        every { userRepository.findByEmail("new@example.com") } returns null
        every { userRepository.save(any()) } returns newUser
        every { tokenService.issue(newUser.id, newUser.email, newUser.role) } returns tokenPair

        val result = service.verifyMagicLink(rawToken)

        assertEquals(tokenPair, result)
        verify(exactly = 1) { userRepository.save(any()) }
    }

    // ── verifyMagicLink — failure paths ──────────────────────────────────────

    @Test
    fun `verifyMagicLink throws when token is not found`() {
        every { magicLinkTokenRepository.claimToken(sha256("unknown")) } returns null

        val ex = assertThrows<IllegalArgumentException> { service.verifyMagicLink("unknown") }
        assertEquals("Invalid magic link token", ex.message)
    }

    @Test
    fun `verifyMagicLink throws when token is expired or already used`() {
        // The atomic DB UPDATE filters both cases; service sees null either way
        every { magicLinkTokenRepository.claimToken(sha256("abc123")) } returns null

        val ex = assertThrows<IllegalArgumentException> { service.verifyMagicLink("abc123") }
        assertEquals("Invalid magic link token", ex.message)
    }

    @Test
    fun `verifyMagicLink concurrent second claim is rejected - simulated race condition`() {
        // The atomic UPDATE ensures only one caller's row-level lock wins.
        // The losing concurrent request sees no rows returned (null) and gets the same error.
        every { magicLinkTokenRepository.claimToken(sha256("abc123")) } returns null

        val ex = assertThrows<IllegalArgumentException> { service.verifyMagicLink("abc123") }
        assertEquals("Invalid magic link token", ex.message)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return String(Hex.encode(digest.digest(input.toByteArray())))
    }
}
