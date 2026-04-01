package com.ureka.play4change.auth.adapter.outbound.persistence

import com.ureka.play4change.auth.adapter.outbound.persistence.entity.RefreshTokenEntity
import com.ureka.play4change.auth.adapter.outbound.persistence.spring.RefreshTokenJpaRepository
import com.ureka.play4change.auth.domain.model.RefreshToken
import com.ureka.play4change.auth.port.outbound.RefreshTokenRepository
import org.springframework.stereotype.Component

@Component
class RefreshTokenJpaAdapter(private val jpa: RefreshTokenJpaRepository) : RefreshTokenRepository {

    override fun save(token: RefreshToken): RefreshToken =
        jpa.save(token.toEntity()).toDomain()

    override fun findByTokenHash(hash: String): RefreshToken? =
        jpa.findByTokenHash(hash)?.toDomain()

    override fun markUsed(id: String) = jpa.markUsed(id)

    override fun revokeAllByUserId(userId: String) = jpa.revokeAllByUserId(userId)

    override fun revokeAllByFamilyId(familyId: String) = jpa.revokeAllByFamilyId(familyId)

    private fun RefreshTokenEntity.toDomain() = RefreshToken(
        id = id, tokenHash = tokenHash, userId = userId,
        familyId = familyId, expiresAt = expiresAt,
        used = used, createdAt = createdAt, role = role
    )

    private fun RefreshToken.toEntity() = RefreshTokenEntity(
        id = id, tokenHash = tokenHash, userId = userId,
        familyId = familyId, expiresAt = expiresAt,
        used = used, createdAt = createdAt, role = role
    )
}
