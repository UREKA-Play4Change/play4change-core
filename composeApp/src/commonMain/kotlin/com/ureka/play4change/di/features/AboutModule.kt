package com.ureka.play4change.di.features

import com.ureka.play4change.features.about.presentation.DefaultAboutComponent
import org.koin.dsl.module

val aboutModule = module {
    factory { (context: com.arkivanov.decompose.ComponentContext) ->
        DefaultAboutComponent(context)
    }
}
