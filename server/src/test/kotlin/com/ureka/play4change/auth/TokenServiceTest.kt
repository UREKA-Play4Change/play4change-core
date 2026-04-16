package com.ureka.play4change.auth

import com.ureka.play4change.auth.application.JwtProperties
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.auth.domain.model.RefreshToken
import com.ureka.play4change.auth.port.outbound.RefreshTokenRepository
import com.ureka.play4change.auth.port.outbound.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.codec.Hex
import java.security.MessageDigest
import java.time.OffsetDateTime

class TokenServiceTest {

    private val refreshTokenRepository: RefreshTokenRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val jwtProperties = JwtProperties(
        secret = "test-secret-that-is-long-enough-for-hmac-sha",
        accessTtlMinutes = 15,
        refreshTtlDays = 7
    )

    private val tokenService = TokenService(
        refreshTokenRepository = refreshTokenRepository,
        userRepository = userRepository,
        jwtProperties = jwtProperties
    )

    // ── revoke ────────────────────────────────────────────────────────────────

    @Test
    fun `revoke invalidates the entire token family`() {
        val rawToken = "some-raw-refresh-token"
        val hash = sha256(rawToken)
        val stored = RefreshToken(
            id = "rt-1",
            tokenHash = hash,
            userId = "user-1",
            familyId = "family-abc",
            expiresAt = OffsetDateTime.now().plusDays(7),
            used = false,
            createdAt = OffsetDateTime.now(),
            role = "USER"
        )
        every { refreshTokenRepository.findByTokenHash(hash) } returns stored

        tokenService.revoke(rawToken)

        verify(exactly = 1) { refreshTokenRepository.revokeAllByFamilyId("family-abc") }
        verify(exactly = 0) { refreshTokenRepository.markUsed(any()) }
    }

    @Test
    fun `revoke does nothing when token is not found`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns null

        tokenService.revoke("unknown-token")

        verify(exactly = 0) { refreshTokenRepository.revokeAllByFamilyId(any()) }
        verify(exactly = 0) { refreshTokenRepository.markUsed(any()) }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return String(Hex.encode(digest.digest(input.toByteArray())))
    }
}
