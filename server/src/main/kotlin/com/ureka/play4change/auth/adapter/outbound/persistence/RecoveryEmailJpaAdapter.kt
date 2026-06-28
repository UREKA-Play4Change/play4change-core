package com.ureka.play4change.auth.adapter.outbound.persistence

import com.ureka.play4change.auth.adapter.outbound.persistence.entity.RecoveryEmailEntity
import com.ureka.play4change.auth.adapter.outbound.persistence.spring.RecoveryEmailJpaRepository
import com.ureka.play4change.auth.domain.model.RecoveryEmail
import com.ureka.play4change.auth.port.outbound.RecoveryEmailRepository
import org.springframework.stereotype.Component

@Component
class RecoveryEmailJpaAdapter(private val jpa: RecoveryEmailJpaRepository) : RecoveryEmailRepository {

    override fun findByEmail(email: String): RecoveryEmail? =
        jpa.findByEmail(email)?.toDomain()

    override fun findVerifiedByEmail(email: String): RecoveryEmail? =
        jpa.findByEmailAndVerifiedTrue(email)?.toDomain()

    override fun findAllByUserId(userId: String): List<RecoveryEmail> =
        jpa.findAllByUserId(userId).map { it.toDomain() }

    override fun save(recoveryEmail: RecoveryEmail): RecoveryEmail =
        jpa.save(recoveryEmail.toEntity()).toDomain()

    override fun delete(id: String, userId: String) =
        jpa.deleteByIdAndUserId(id, userId)

    override fun claimVerificationToken(tokenHash: String): RecoveryEmail? {
        val email = jpa.claimVerificationToken(tokenHash) ?: return null
        return jpa.findByEmail(email)?.toDomain()
    }

    private fun RecoveryEmailEntity.toDomain() = RecoveryEmail(
        id = id, userId = userId, email = email,
        verified = verified, tokenHash = tokenHash,
        tokenExpiresAt = tokenExpiresAt, createdAt = createdAt
    )

    private fun RecoveryEmail.toEntity() = RecoveryEmailEntity(
        id = id, userId = userId, email = email,
        verified = verified, tokenHash = tokenHash,
        tokenExpiresAt = tokenExpiresAt, createdAt = createdAt
    )
}