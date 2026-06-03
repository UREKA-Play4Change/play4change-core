package com.ureka.play4change.di

import com.ureka.play4change.core.isDebugBuild
import com.ureka.play4change.core.network.KeychainTokenStorage
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSProcessInfo

actual val platformModule: Module = module {
    single {
        // Simulator shares the Mac's loopback — localhost works there.
        // On a physical device localhost points to the device itself, so we
        // must use the public Cloudflare tunnel URL instead.
        val isSimulator = NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null
        val baseUrl = if (isDebugBuild && isSimulator) "http://localhost/play4change-server"
                      else "https://radesh-govind.com/play4change-server"
        NetworkConfig(baseUrl)
    }
    single<TokenStorage> { KeychainTokenStorage() }
}
