package com.ureka.play4change.di.features

import com.ureka.play4change.features.home.data.mock.MockHomeRepository
import com.ureka.play4change.features.home.domain.repository.HomeRepository
import com.ureka.play4change.features.home.presentation.DefaultHomeComponent
import org.koin.dsl.module

val homeModule = module {
    single<HomeRepository> { MockHomeRepository() }
    factory { (context: com.arkivanov.decompose.ComponentContext) ->
        DefaultHomeComponent(context, get())
    }
}
