package com.ureka.play4change.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin module.
 * Android provides: [NetworkConfig] (from BuildConfig.BASE_URL) + [TokenStorage]
 *   (EncryptedSharedPreferencesTokenStorage).
 * iOS provides: [NetworkConfig] (localhost:8080 for simulator) + [TokenStorage]
 *   (KeychainTokenStorage).
 */
expect val platformModule: Module
