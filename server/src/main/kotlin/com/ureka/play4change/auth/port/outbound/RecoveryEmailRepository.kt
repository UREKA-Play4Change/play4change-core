package com.ureka.play4change.auth.port.outbound

import com.ureka.play4change.auth.domain.model.RecoveryEmail

interface RecoveryEmailRepository {
    fun findByEmail(email: String): RecoveryEmail?
    fun findVerifiedByEmail(email: String): RecoveryEmail?
    fun findAllByUserId(userId: String): List<RecoveryEmail>
    fun save(recoveryEmail: RecoveryEmail): RecoveryEmail
    fun delete(id: String, userId: String)
    /**
     * Atomically marks the row as verified and clears the token fields using a single
     * UPDATE … RETURNING statement. Returns the updated [RecoveryEmail] if the token was
     * valid and not yet expired; null otherwise.
     */
    fun claimVerificationToken(tokenHash: String): RecoveryEmail?
}