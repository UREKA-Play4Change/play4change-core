package com.ureka.play4change.di.features

import com.ureka.play4change.features.splash.data.mock.MockSplashRepository
import com.ureka.play4change.features.splash.domain.repository.SplashRepository
import com.ureka.play4change.features.splash.presentation.DefaultSplashComponent
import org.koin.dsl.module

val splashModule = module {
    single<SplashRepository> { MockSplashRepository() }
    factory { (context: com.arkivanov.decompose.ComponentContext) ->
        DefaultSplashComponent(context, get())
    }
}
