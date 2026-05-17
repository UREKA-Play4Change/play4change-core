package com.ureka.play4change.domain.notification

import java.time.OffsetDateTime

data class DeviceToken(
    val id: String,
    val userId: String,
    val token: String,
    val platform: DeviceTokenPlatform,
    val lastNotifiedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)

enum class DeviceTokenPlatform { ANDROID, IOS }
