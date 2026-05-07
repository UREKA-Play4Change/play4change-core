package com.ureka.play4change.di.features

import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.features.auth.data.http.HttpAuthRepository
import com.ureka.play4change.features.auth.data.mock.MockAuthRepository
import com.ureka.play4change.features.auth.domain.repository.AuthRepository
import com.ureka.play4change.features.auth.presentation.DefaultLoginComponent
import io.ktor.client.HttpClient
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> {
        val config = get<NetworkConfig>()
        if (config.useMocks) MockAuthRepository()
        else HttpAuthRepository(get<HttpClient>(), get())
    }
    factory { (context: com.arkivanov.decompose.ComponentContext) ->
        DefaultLoginComponent(context, get())
    }
}
