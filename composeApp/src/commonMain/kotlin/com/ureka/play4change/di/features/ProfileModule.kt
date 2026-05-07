package com.ureka.play4change.di.features

import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.TokenStorage
import com.ureka.play4change.features.profile.data.http.HttpProfileRepository
import com.ureka.play4change.features.profile.data.mock.MockProfileRepository
import com.ureka.play4change.features.profile.domain.repository.ProfileRepository
import com.ureka.play4change.features.profile.presentation.DefaultProfileComponent
import io.ktor.client.HttpClient
import org.koin.dsl.module

val profileModule = module {
    single<ProfileRepository> {
        val config = get<NetworkConfig>()
        if (config.useMocks) MockProfileRepository()
        else HttpProfileRepository(get<HttpClient>(), get<TokenStorage>())
    }
    factory { (context: com.arkivanov.decompose.ComponentContext) ->
        DefaultProfileComponent(context, get())
    }
}
