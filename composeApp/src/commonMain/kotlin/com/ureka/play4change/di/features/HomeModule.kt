package com.ureka.play4change.di.features

import com.ureka.play4change.features.home.data.http.HttpHomeRepository
import com.ureka.play4change.features.home.domain.repository.HomeRepository
import com.ureka.play4change.features.home.presentation.DefaultHomeComponent
import org.koin.dsl.module

val homeModule = module {
    single<HomeRepository> { HttpHomeRepository(get(), get()) }
    factory { (context: com.arkivanov.decompose.ComponentContext) ->
        DefaultHomeComponent(context, get(), get(), get())
    }
}
