package com.ureka.play4change.di

import com.ureka.play4change.core.network.KeychainTokenStorage
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { NetworkConfig("https://radesh-govind.com/play4change-server") }
    single<TokenStorage> { KeychainTokenStorage() }
}
