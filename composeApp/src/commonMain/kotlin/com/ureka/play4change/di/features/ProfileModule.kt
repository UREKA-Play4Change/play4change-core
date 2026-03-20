package com.ureka.play4change.di.features

import com.ureka.play4change.features.profile.data.mock.MockProfileRepository
import com.ureka.play4change.features.profile.domain.repository.ProfileRepository
import com.ureka.play4change.features.profile.presentation.DefaultProfileComponent
import org.koin.dsl.module

val profileModule = module {
    single<ProfileRepository> { MockProfileRepository() }
    factory { (context: com.arkivanov.decompose.ComponentContext) ->
        DefaultProfileComponent(context, get())
    }
}
