package com.ureka.play4change.di

import com.ureka.play4change.core.network.KeychainTokenStorage
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    // iOS simulator connects to host machine via localhost; real device needs the LAN IP.
    single { NetworkConfig("http://localhost:8080") }
    single<TokenStorage> { KeychainTokenStorage() }
}
