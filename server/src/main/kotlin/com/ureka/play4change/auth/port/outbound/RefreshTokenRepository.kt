package com.ureka.play4change.auth.port.outbound

import com.ureka.play4change.auth.domain.model.RefreshToken

interface RefreshTokenRepository {
    fun save(token: RefreshToken): RefreshToken
    fun findByTokenHash(hash: String): RefreshToken?
    fun markUsed(id: String)
    fun revokeAllByUserId(userId: String)
    fun revokeAllByFamilyId(familyId: String)
}
