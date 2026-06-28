package com.ureka.play4change.auth.application

import com.ureka.play4change.auth.domain.crypto.AuthCrypto
import com.ureka.play4change.auth.domain.model.RecoveryEmail
import com.ureka.play4change.auth.port.outbound.EmailPort
import com.ureka.play4change.auth.port.outbound.RecoveryEmailRepository
import com.ureka.play4change.auth.port.outbound.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class RecoveryEmailService(
    private val recoveryEmailRepository: RecoveryEmailRepository,
    private val userRepository: UserRepository,
    private val emailPort: EmailPort,
    private val clock: Clock
) {
    fun listForUser(userId: String): List<RecoveryEmail> =
        recoveryEmailRepository.findAllByUserId(userId)

    @Transactional
    fun addRecoveryEmail(userId: String, email: String) {
        val normalised = email.lowercase().trim()

        val owner = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")
        if (owner.email == normalised) {
            throw IllegalArgumentException("This is already your primary sign-in email")
        }
        if (userRepository.existsByEmail(normalised)) {
            throw IllegalArgumentException("Email is already registered as a primary account email")
        }
        if (recoveryEmailRepository.findByEmail(normalised) != null) {
            throw IllegalArgumentException("Email is already registered as a recovery email")
        }
        if (recoveryEmailRepository.findAllByUserId(userId).size >= MAX_RECOVERY_EMAILS) {
            throw IllegalArgumentException("You can have at most $MAX_RECOVERY_EMAILS recovery emails")
        }

        val rawToken = AuthCrypto.generateOpaqueToken()
        recoveryEmailRepository.save(
            RecoveryEmail(
                id = UUID.randomUUID().toString(),
                userId = userId,
                email = normalised,
                verified = false,
                tokenHash = AuthCrypto.sha256Hex(rawToken),
                tokenExpiresAt = OffsetDateTime.now(clock).plusHours(24),
                createdAt = OffsetDateTime.now(clock)
            )
        )
        emailPort.sendRecoveryEmailVerification(normalised, rawToken)
    }

    @Transactional
    fun verifyRecoveryEmail(token: String): RecoveryEmail =
        recoveryEmailRepository.claimVerificationToken(AuthCrypto.sha256Hex(token))
            ?: throw IllegalArgumentException("Invalid or expired verification token")

    fun removeRecoveryEmail(userId: String, recoveryEmailId: String) =
        recoveryEmailRepository.delete(recoveryEmailId, userId)

    private companion object {
        const val MAX_RECOVERY_EMAILS = 3
    }
}