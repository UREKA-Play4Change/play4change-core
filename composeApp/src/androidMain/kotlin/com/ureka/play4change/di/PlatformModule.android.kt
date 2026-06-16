package com.ureka.play4change.di

import com.ureka.play4change.BuildConfig
import com.ureka.play4change.core.network.EncryptedSharedPreferencesTokenStorage
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.net.HttpURLConnection
import java.net.URL

// Emulator routes 10.0.2.2 to the host machine (your Mac).
// On a real device this will always fail, so the production URL is used instead.
private const val FALLBACK_URL = "http://10.0.2.2/play4change-server"
private const val PROBE_TIMEOUT_MS = 1500

actual val platformModule: Module = module {
    single {
        val primary = BuildConfig.BASE_URL.takeIf { it.isNotBlank() }
            ?: "https://radesh-govind.com/play4change-server"
        val baseUrl = runBlocking { pickBaseUrl(primary, FALLBACK_URL) }
        NetworkConfig(baseUrl, BuildConfig.USE_MOCKS)
    }
    single<TokenStorage> { EncryptedSharedPreferencesTokenStorage(androidContext()) }
}

private suspend fun pickBaseUrl(primary: String, fallback: String): String =
    withContext(Dispatchers.IO) {
        val primaryJob = async { probe(primary) }
        val fallbackJob = async { probe(fallback) }
        when {
            primaryJob.await() -> { fallbackJob.cancel(); primary }
            fallbackJob.await() -> fallback
            else -> primary
        }
    }

private fun probe(baseUrl: String): Boolean = try {
    val conn = URL("$baseUrl/actuator/health").openConnection() as HttpURLConnection
    conn.connectTimeout = PROBE_TIMEOUT_MS
    conn.readTimeout = PROBE_TIMEOUT_MS
    conn.requestMethod = "GET"
    val code = conn.responseCode
    conn.disconnect()
    code in 200..599
} catch (_: Exception) {
    false
}
