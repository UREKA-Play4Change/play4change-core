package com.ureka.play4change.auth.port.outbound

import com.ureka.play4change.auth.domain.model.MagicLinkToken

interface MagicLinkTokenRepository {
    fun save(token: MagicLinkToken): MagicLinkToken
    fun findByToken(token: String): MagicLinkToken?
    fun markUsed(id: String)
    /** Atomically marks the token used and returns the email, or null if token is invalid/expired/already used. */
    fun claimToken(tokenHash: String): String?
}
