package com.ureka.play4change.domain.notification

import java.time.OffsetDateTime

interface DeviceTokenRepository {
    fun upsert(userId: String, token: String, platform: DeviceTokenPlatform): DeviceToken
    fun deleteAllByUserId(userId: String)
    fun findByUserId(userId: String): List<DeviceToken>
    fun findAll(): List<DeviceToken>
    fun updateLastNotifiedAt(id: String, at: OffsetDateTime)
}
