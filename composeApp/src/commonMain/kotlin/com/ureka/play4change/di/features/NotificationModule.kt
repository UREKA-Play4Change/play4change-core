package com.ureka.play4change.di.features

import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.features.notification.data.http.HttpNotificationRepository
import com.ureka.play4change.features.notification.domain.repository.NotificationRepository
import io.ktor.client.HttpClient
import org.koin.dsl.module

val notificationModule = module {
    single<NotificationRepository> {
        val config = get<NetworkConfig>()
        if (config.useMocks) {
            object : NotificationRepository {
                override suspend fun registerDeviceToken(token: String, platform: String) = Unit
            }
        } else {
            HttpNotificationRepository(get<HttpClient>())
        }
    }
}
