package com.ureka.play4change.application.port

import com.ureka.play4change.domain.notification.DeviceToken

/**
 * Port for sending push notifications to mobile devices.
 * ANDROID tokens are delivered via FCM HTTP v1 API.
 * IOS tokens are delivered via APNs HTTP/2 provider API.
 */
interface PushNotificationPort {
    fun send(deviceToken: DeviceToken, title: String, body: String)
}
