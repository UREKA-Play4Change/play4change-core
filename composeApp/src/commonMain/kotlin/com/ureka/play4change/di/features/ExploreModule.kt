package com.ureka.play4change.di.features

import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.features.explore.data.http.HttpExploreRepository
import com.ureka.play4change.features.explore.data.mock.MockExploreRepository
import com.ureka.play4change.features.explore.domain.repository.ExploreRepository
import com.ureka.play4change.features.explore.presentation.DefaultExploreComponent
import io.ktor.client.HttpClient
import org.koin.dsl.module

val exploreModule = module {
    single<ExploreRepository> {
        val config = get<NetworkConfig>()
        if (config.useMocks) MockExploreRepository()
        else HttpExploreRepository(get<HttpClient>())
    }
    factory { (context: com.arkivanov.decompose.ComponentContext,
               onNavigateBack: () -> Unit) ->
        DefaultExploreComponent(context, get(), onNavigateBack)
    }
}
