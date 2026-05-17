package com.ureka.play4change.application.notification

import com.ureka.play4change.domain.notification.DeviceToken
import com.ureka.play4change.domain.notification.DeviceTokenPlatform
import com.ureka.play4change.domain.notification.DeviceTokenRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class DeviceTokenServiceTest {

    private val deviceTokenRepository = mockk<DeviceTokenRepository>()
    private val service = DeviceTokenService(deviceTokenRepository)

    private val userId = "user-1"
    private val token = "fcm-token-abc"

    private fun aDeviceToken(platform: DeviceTokenPlatform = DeviceTokenPlatform.ANDROID) = DeviceToken(
        id = "dt-1",
        userId = userId,
        token = token,
        platform = platform,
        lastNotifiedAt = null,
        createdAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now()
    )

    @Test
    fun `register with new userId+platform creates a device token record`() {
        every { deviceTokenRepository.upsert(userId, token, DeviceTokenPlatform.ANDROID) } returns aDeviceToken()

        service.register(userId, token, "ANDROID")

        verify(exactly = 1) { deviceTokenRepository.upsert(userId, token, DeviceTokenPlatform.ANDROID) }
    }

    @Test
    fun `register with existing userId+platform replaces the token (upsert)`() {
        val newToken = "fcm-token-new"
        every { deviceTokenRepository.upsert(userId, newToken, DeviceTokenPlatform.IOS) } returns
            aDeviceToken(DeviceTokenPlatform.IOS)

        service.register(userId, newToken, "IOS")

        verify(exactly = 1) { deviceTokenRepository.upsert(userId, newToken, DeviceTokenPlatform.IOS) }
    }

    @Test
    fun `deleteForUser removes all device tokens for the user`() {
        justRun { deviceTokenRepository.deleteAllByUserId(userId) }

        service.deleteForUser(userId)

        verify(exactly = 1) { deviceTokenRepository.deleteAllByUserId(userId) }
    }
}
