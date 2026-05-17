package com.ureka.play4change.application.notification

import com.ureka.play4change.application.port.DeviceTokenUseCase
import com.ureka.play4change.domain.notification.DeviceTokenPlatform
import com.ureka.play4change.domain.notification.DeviceTokenRepository
import org.springframework.stereotype.Service

@Service
class DeviceTokenService(
    private val deviceTokenRepository: DeviceTokenRepository
) : DeviceTokenUseCase {

    override fun register(userId: String, token: String, platform: String) {
        val devicePlatform = runCatching { DeviceTokenPlatform.valueOf(platform.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Invalid platform: $platform") }
        deviceTokenRepository.upsert(userId, token, devicePlatform)
    }

    override fun deleteForUser(userId: String) {
        deviceTokenRepository.deleteAllByUserId(userId)
    }
}
