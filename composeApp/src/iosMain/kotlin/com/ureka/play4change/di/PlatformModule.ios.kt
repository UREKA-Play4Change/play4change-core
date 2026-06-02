package com.ureka.play4change.di

import com.ureka.play4change.core.isDebugBuild
import com.ureka.play4change.core.network.KeychainTokenStorage
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single {
        // Simulator shares the Mac's loopback — no tunnel needed for debug builds.
        // Release / TestFlight builds hit the Cloudflare tunnel URL.
        val baseUrl = if (isDebugBuild) "http://localhost/play4change-server"
                      else "https://radesh-govind.com/play4change-server"
        NetworkConfig(baseUrl)
    }
    single<TokenStorage> { KeychainTokenStorage() }
}
