package com.ureka.play4change.di

import com.ureka.play4change.BuildConfig
import com.ureka.play4change.core.network.EncryptedSharedPreferencesTokenStorage
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.TokenStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { NetworkConfig(BuildConfig.BASE_URL, BuildConfig.USE_MOCKS) }
    single<TokenStorage> { EncryptedSharedPreferencesTokenStorage(androidContext()) }
}
