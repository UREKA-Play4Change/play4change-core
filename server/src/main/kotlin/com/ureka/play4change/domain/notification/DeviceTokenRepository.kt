package com.ureka.play4change.domain.notification

interface DeviceTokenRepository {
    fun upsert(userId: String, token: String, platform: DeviceTokenPlatform): DeviceToken
    fun deleteAllByUserId(userId: String)
    fun findByUserId(userId: String): List<DeviceToken>
}
