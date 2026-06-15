package com.ureka.play4change.auth.application

import com.ureka.play4change.auth.AuthProvider
import com.ureka.play4change.auth.domain.crypto.AuthCrypto
import com.ureka.play4change.auth.domain.model.MagicLinkToken
import com.ureka.play4change.auth.domain.model.TokenPair
import com.ureka.play4change.auth.domain.model.AuthUser
import com.ureka.play4change.auth.port.inbound.AuthUseCase
import com.ureka.play4change.auth.port.outbound.EmailPort
import com.ureka.play4change.auth.port.outbound.MagicLinkTokenRepository
import com.ureka.play4change.auth.port.outbound.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MagicLinkService(
    private val magicLinkTokenRepository: MagicLinkTokenRepository,
    private val userRepository: UserRepository,
    private val emailPort: EmailPort,
    private val tokenService: TokenService,
    private val clock: Clock
) : AuthUseCase {

    override fun requestMagicLink(email: String) {
        val normalised = email.lowercase().trim()
        val rawToken = AuthCrypto.generateOpaqueToken()
        // Store the SHA-256 hash — the raw token only travels in the email link
        magicLinkTokenRepository.save(
            MagicLinkToken(
                id = UUID.randomUUID().toString(),
                token = AuthCrypto.sha256Hex(rawToken),
                email = normalised,
                expiresAt = OffsetDateTime.now(clock).plusMinutes(15),
                used = false,
                createdAt = OffsetDateTime.now(clock)
            )
        )
        emailPort.sendMagicLink(normalised, rawToken)
    }

    @Transactional
    override fun verifyMagicLink(token: String): TokenPair {
        // Atomic UPDATE: marks used=true only if token exists, is unused, and not expired.
        // DB-level atomicity closes the TOCTOU window — no separate check-then-update.
        val email = magicLinkTokenRepository.claimToken(AuthCrypto.sha256Hex(token))
            ?: throw IllegalArgumentException("Invalid magic link token")

        val user = userRepository.findByEmail(email)
            ?: userRepository.save(
                AuthUser(
                    id = UUID.randomUUID().toString(),
                    email = email,
                    name = null,
                    provider = AuthProvider.MAGIC_LINK,
                    providerId = null,
                    createdAt = OffsetDateTime.now(clock)
                )
            )

        return tokenService.issue(user.id, user.email, user.role)
    }
}
