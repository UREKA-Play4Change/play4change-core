package com.ureka.play4change.di.features

import com.ureka.play4change.features.explore.data.mock.MockExploreRepository
import com.ureka.play4change.features.explore.domain.repository.ExploreRepository
import com.ureka.play4change.features.explore.presentation.DefaultExploreComponent
import org.koin.dsl.module

val exploreModule = module {
    single<ExploreRepository> { MockExploreRepository() }
    factory { (context: com.arkivanov.decompose.ComponentContext,
               onNavigateBack: () -> Unit) ->
        DefaultExploreComponent(context, get(), onNavigateBack)
    }
}
