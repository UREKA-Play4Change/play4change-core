package com.ureka.play4change.di

import com.ureka.play4change.di.features.aboutModule
import com.ureka.play4change.di.features.authModule
import com.ureka.play4change.di.features.exploreModule
import com.ureka.play4change.di.features.homeModule
import com.ureka.play4change.di.features.profileModule
import com.ureka.play4change.di.features.splashModule
import com.ureka.play4change.di.features.explanationModule
import com.ureka.play4change.di.features.struggleModule
import com.ureka.play4change.di.features.taskModule
import org.koin.dsl.module

// TODO: re-add peerReviewModule once presentation/ and ui/ layers are implemented
val appModule = module {
    includes(
        platformModule, coreModule,
        splashModule, authModule, homeModule, taskModule,
        profileModule, aboutModule, exploreModule,
        struggleModule, explanationModule
    )
}
