package com.ureka.play4change.di

import com.ureka.play4change.di.features.aboutModule
import com.ureka.play4change.di.features.authModule
import com.ureka.play4change.di.features.homeModule
import com.ureka.play4change.di.features.profileModule
import com.ureka.play4change.di.features.splashModule
import com.ureka.play4change.di.features.taskModule
import org.koin.dsl.module

val appModule = module {
    includes(coreModule, splashModule, authModule, homeModule, taskModule, profileModule, aboutModule)
}
