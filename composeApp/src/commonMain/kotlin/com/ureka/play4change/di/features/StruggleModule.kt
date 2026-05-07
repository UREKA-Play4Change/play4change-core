package com.ureka.play4change.di.features

import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.features.struggle.data.http.HttpStruggleRepository
import com.ureka.play4change.features.struggle.data.mock.MockStruggleRepository
import com.ureka.play4change.features.struggle.domain.repository.StruggleRepository
import io.ktor.client.HttpClient
import org.koin.dsl.module

val struggleModule = module {
    single<StruggleRepository> {
        val config = get<NetworkConfig>()
        if (config.useMocks) MockStruggleRepository()
        else HttpStruggleRepository(get<HttpClient>())
    }
}
