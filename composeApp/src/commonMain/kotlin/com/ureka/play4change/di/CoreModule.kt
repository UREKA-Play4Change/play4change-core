package com.ureka.play4change.di

import com.ureka.play4change.core.network.HttpClientFactory
import com.ureka.play4change.core.network.SessionEventBus
import org.koin.dsl.module

val coreModule = module {
    single {
        HttpClientFactory.create(
            tokenStorage = get(),
            networkConfig = get(),
            onSessionExpired = { SessionEventBus.sessionExpired() }
        )
    }
}
