package com.ureka.play4change.di.features

import com.ureka.play4change.features.auth.data.mock.MockAuthRepository
import com.ureka.play4change.features.auth.domain.repository.AuthRepository
import com.ureka.play4change.features.auth.presentation.DefaultLoginComponent
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> { MockAuthRepository() }
    factory { (context: com.arkivanov.decompose.ComponentContext,
               onNavigateToAbout: () -> Unit) ->
        DefaultLoginComponent(context, get(), onNavigateToAbout)
    }
}
