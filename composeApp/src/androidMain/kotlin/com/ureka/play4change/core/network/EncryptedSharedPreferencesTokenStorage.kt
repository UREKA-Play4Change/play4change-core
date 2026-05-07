package com.ureka.play4change.core.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

private const val PREFS_FILE = "play4change_secure_tokens"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"

/**
 * Stores JWT tokens in [EncryptedSharedPreferences].
 *
 * Key encryption:   AES256_SIV  (nonce-free, deterministic — safe for key names)
 * Value encryption: AES256_GCM  (authenticated encryption for values)
 *
 * The master key is created in the Android Keystore and never leaves it.
 * Plain SharedPreferences must never be used for tokens. See THREAT-LOG R-Phase04.
 */
class EncryptedSharedPreferencesTokenStorage(context: Context) : TokenStorage {

    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_FILE,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun getAccessToken(): String? =
        prefs.getString(KEY_ACCESS_TOKEN, null)

    override suspend fun getRefreshToken(): String? =
        prefs.getString(KEY_REFRESH_TOKEN, null)

    override suspend fun store(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    override suspend fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }
}
