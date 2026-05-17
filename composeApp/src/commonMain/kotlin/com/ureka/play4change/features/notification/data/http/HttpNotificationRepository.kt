package com.ureka.play4change.features.notification.data.http

import com.ureka.play4change.features.notification.domain.repository.NotificationRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
private data class RegisterDeviceTokenRequest(val token: String, val platform: String)

class HttpNotificationRepository(private val client: HttpClient) : NotificationRepository {

    override suspend fun registerDeviceToken(token: String, platform: String) {
        runCatching {
            client.post("/notifications/device-token") {
                contentType(ContentType.Application.Json)
                setBody(RegisterDeviceTokenRequest(token, platform))
            }
        }
    }
}
