package com.ureka.play4change.auth.application

import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.MagicLinkToken
import com.ureka.play4change.auth.domain.model.TokenPair
import com.ureka.play4change.auth.domain.model.User
import com.ureka.play4change.auth.port.inbound.AuthUseCase
import com.ureka.play4change.auth.port.outbound.EmailPort
import com.ureka.play4change.auth.port.outbound.MagicLinkTokenRepository
import com.ureka.play4change.auth.port.outbound.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.codec.Hex
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MagicLinkService(
    private val magicLinkTokenRepository: MagicLinkTokenRepository,
    private val userRepository: UserRepository,
    private val emailPort: EmailPort,
    private val tokenService: TokenService,
    @Value("\${app.base-url}") private val baseUrl: String
) : AuthUseCase {

    override fun requestMagicLink(email: String) {
        val normalised = email.lowercase().trim()
        val rawToken = generateToken()
        // Store the SHA-256 hash — the raw token only travels in the email link
        val tokenHash = sha256(rawToken)
        magicLinkTokenRepository.save(
            MagicLinkToken(
                id = UUID.randomUUID().toString(),
                token = tokenHash,
                email = normalised,
                expiresAt = OffsetDateTime.now().plusMinutes(15),
                used = false,
                createdAt = OffsetDateTime.now()
            )
        )
        emailPort.sendMagicLink(normalised, "$baseUrl/auth/verify?token=$rawToken")
    }

    @Transactional
    override fun verifyMagicLink(token: String): TokenPair {
        val tokenHash = sha256(token)
        // Atomic UPDATE: marks used=true only if token exists, is unused, and not expired.
        // DB-level atomicity closes the TOCTOU window — no separate check-then-update.
        val email = magicLinkTokenRepository.claimToken(tokenHash)
            ?: throw IllegalArgumentException("Invalid magic link token")

        val user = userRepository.findByEmail(email)
            ?: userRepository.save(
                User(
                    id = UUID.randomUUID().toString(),
                    email = email,
                    name = null,
                    provider = AuthProvider.MAGIC_LINK,
                    providerId = null,
                    createdAt = OffsetDateTime.now()
                )
            )

        return tokenService.issue(user.id, user.email, user.role)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return String(Hex.encode(bytes))
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return String(Hex.encode(digest.digest(input.toByteArray())))
    }
}
