package com.ureka.play4change.auth.adapter.outbound.persistence.spring

import com.ureka.play4change.auth.adapter.outbound.persistence.entity.RecoveryEmailEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface RecoveryEmailJpaRepository : JpaRepository<RecoveryEmailEntity, String> {

    fun findByEmail(email: String): RecoveryEmailEntity?

    fun findByEmailAndVerifiedTrue(email: String): RecoveryEmailEntity?

    fun findAllByUserId(userId: String): List<RecoveryEmailEntity>

    @Modifying
    @Transactional
    @Query("DELETE FROM RecoveryEmailEntity r WHERE r.id = :id AND r.userId = :userId")
    fun deleteByIdAndUserId(@Param("id") id: String, @Param("userId") userId: String)

    /**
     * Atomically marks the row verified and clears the token fields.
     * Uses UPDATE … RETURNING so the check-and-update is a single DB round trip,
     * matching the same pattern used by [MagicLinkTokenJpaRepository.claimToken].
     * Returns the email of the claimed row, or null if the token was invalid or expired.
     */
    @Transactional
    @Query(
        value = """
            WITH updated AS (
                UPDATE recovery_emails
                SET verified = true, token_hash = NULL, token_expires_at = NULL
                WHERE token_hash = :tokenHash AND verified = false AND token_expires_at > now()
                RETURNING email
            )
            SELECT email FROM updated
        """,
        nativeQuery = true
    )
    fun claimVerificationToken(@Param("tokenHash") tokenHash: String): String?
}