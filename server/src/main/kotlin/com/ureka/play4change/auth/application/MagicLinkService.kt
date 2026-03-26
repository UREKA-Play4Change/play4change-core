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
        magicLinkTokenRepository.save(
            MagicLinkToken(
                id = UUID.randomUUID().toString(),
                token = rawToken,
                email = normalised,
                expiresAt = OffsetDateTime.now().plusMinutes(15),
                used = false,
                createdAt = OffsetDateTime.now()
            )
        )
        emailPort.sendMagicLink(normalised, "$baseUrl/auth/verify?token=$rawToken")
    }

    override fun verifyMagicLink(token: String): TokenPair {
        val stored = magicLinkTokenRepository.findByToken(token)
            ?: throw IllegalArgumentException("Invalid magic link token")

        if (!stored.isValid()) {
            throw IllegalArgumentException("Magic link expired or already used")
        }

        magicLinkTokenRepository.markUsed(stored.id)

        val user = userRepository.findByEmail(stored.email)
            ?: userRepository.save(
                User(
                    id = UUID.randomUUID().toString(),
                    email = stored.email,
                    name = null,
                    provider = AuthProvider.MAGIC_LINK,
                    providerId = null,
                    createdAt = OffsetDateTime.now()
                )
            )

        return tokenService.issue(user.id, user.email)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return String(Hex.encode(bytes))
    }
}
