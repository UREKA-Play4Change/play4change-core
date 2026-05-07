package com.ureka.play4change.core.network

/**
 * Platform-agnostic interface for secure token persistence.
 *
 * Access token: short-lived JWT kept in memory only; written here after refresh.
 * Refresh token: long-lived opaque token that MUST survive process death.
 * Platform implementations must use encrypted storage (EncryptedSharedPreferences
 * on Android, Keychain on iOS). Plain SharedPreferences or UserDefaults are forbidden.
 */
interface TokenStorage {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun store(accessToken: String, refreshToken: String)
    suspend fun clear()
}
