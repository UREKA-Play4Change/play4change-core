package com.ureka.play4change.di

import com.ureka.play4change.core.network.KeychainTokenStorage
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSBundle

actual val platformModule: Module = module {
    single {
        val baseUrl = NSBundle.mainBundle.objectForInfoDictionaryKey("BASE_URL") as? String
            ?: "http://localhost:8080"
        NetworkConfig(baseUrl)
    }
    single<TokenStorage> { KeychainTokenStorage() }
}
