package com.ureka.play4change.auth.port.outbound

import com.ureka.play4change.auth.domain.model.MagicLinkToken

interface MagicLinkTokenRepository {
    fun save(token: MagicLinkToken): MagicLinkToken
    fun findByToken(token: String): MagicLinkToken?
    fun markUsed(id: String)
}
