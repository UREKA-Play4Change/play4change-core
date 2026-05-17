package com.ureka.play4change.features.notification.domain.repository

interface NotificationRepository {
    suspend fun registerDeviceToken(token: String, platform: String)
}
