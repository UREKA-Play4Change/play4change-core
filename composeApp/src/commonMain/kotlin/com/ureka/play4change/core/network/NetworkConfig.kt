package com.ureka.play4change.core.network

/**
 * Network configuration provided by each platform via its Koin [platformModule].
 * Android reads [baseUrl] from BuildConfig.BASE_URL (set in gradle.properties or CI).
 * iOS uses the simulator loopback constant.
 */
/**
 * @param baseUrl   Base URL for all HTTP calls. Android reads from BuildConfig.BASE_URL.
 * @param useMocks  When true the DI graph binds mock repositories instead of HTTP ones.
 *                  Default false — set true only in debug builds for offline development.
 */
data class NetworkConfig(val baseUrl: String, val useMocks: Boolean = false)
