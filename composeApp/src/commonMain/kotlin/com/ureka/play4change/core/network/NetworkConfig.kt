package com.ureka.play4change.core.network

/**
 * Network configuration provided by each platform via its Koin [platformModule].
 * Android reads [baseUrl] from BuildConfig.BASE_URL (set in gradle.properties or CI).
 * iOS uses the simulator loopback constant.
 */
data class NetworkConfig(val baseUrl: String)
