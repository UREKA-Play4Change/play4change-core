package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.notification.DeviceToken
import com.ureka.play4change.domain.notification.DeviceTokenPlatform
import com.ureka.play4change.domain.notification.DeviceTokenRepository
import com.ureka.play4change.infrastructure.persistence.entity.DeviceTokenEntity
import com.ureka.play4change.infrastructure.persistence.repository.DeviceTokenJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Component
class DeviceTokenRepositoryAdapter(
    private val jpa: DeviceTokenJpaRepository
) : DeviceTokenRepository {

    @Transactional
    override fun upsert(userId: String, token: String, platform: DeviceTokenPlatform): DeviceToken {
        val existing = jpa.findByUserIdAndPlatform(userId, platform.name)
        return if (existing != null) {
            existing.token = token
            existing.updatedAt = OffsetDateTime.now()
            jpa.save(existing).toDomain()
        } else {
            jpa.save(DeviceTokenEntity(userId = userId, token = token, platform = platform.name)).toDomain()
        }
    }

    @Transactional
    override fun deleteAllByUserId(userId: String) {
        jpa.deleteAllByUserId(userId)
    }

    override fun findByUserId(userId: String): List<DeviceToken> =
        jpa.findByUserId(userId).map { it.toDomain() }

    private fun DeviceTokenEntity.toDomain() = DeviceToken(
        id = id,
        userId = userId,
        token = token,
        platform = DeviceTokenPlatform.valueOf(platform),
        lastNotifiedAt = lastNotifiedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
