package com.ureka.play4change.di

import com.ureka.play4change.core.network.HttpClientFactory
import com.ureka.play4change.core.network.SessionEventBus
import com.ureka.play4change.features.task.data.TaskCache
import org.koin.dsl.module

val coreModule = module {
    single {
        HttpClientFactory.create(
            tokenStorage = get(),
            networkConfig = get(),
            onSessionExpired = { SessionEventBus.sessionExpired() }
        )
    }
    single { TaskCache() }
}
