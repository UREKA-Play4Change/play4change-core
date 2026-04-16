package com.ureka.play4change.auth.adapter.outbound.persistence

import com.ureka.play4change.auth.adapter.outbound.persistence.entity.MagicLinkTokenEntity
import com.ureka.play4change.auth.adapter.outbound.persistence.spring.MagicLinkTokenJpaRepository
import com.ureka.play4change.auth.domain.model.MagicLinkToken
import com.ureka.play4change.auth.port.outbound.MagicLinkTokenRepository
import org.springframework.stereotype.Component

@Component
class MagicLinkTokenJpaAdapter(private val jpa: MagicLinkTokenJpaRepository) : MagicLinkTokenRepository {

    override fun save(token: MagicLinkToken): MagicLinkToken =
        jpa.save(token.toEntity()).toDomain()

    override fun findByToken(token: String): MagicLinkToken? =
        jpa.findByToken(token)?.toDomain()

    override fun markUsed(id: String) = jpa.markUsed(id)

    override fun claimToken(tokenHash: String): String? = jpa.claimToken(tokenHash)

    private fun MagicLinkTokenEntity.toDomain() = MagicLinkToken(
        id = id, token = token, email = email,
        expiresAt = expiresAt, used = used, createdAt = createdAt
    )

    private fun MagicLinkToken.toEntity() = MagicLinkTokenEntity(
        id = id, token = token, email = email,
        expiresAt = expiresAt, used = used, createdAt = createdAt
    )
}
