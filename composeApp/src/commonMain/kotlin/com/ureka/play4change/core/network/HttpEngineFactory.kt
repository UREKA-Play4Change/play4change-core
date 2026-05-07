package com.ureka.play4change.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Creates a platform-specific [HttpClient] with the given configuration block.
 * Android actual uses OkHttp; iOS actual uses Darwin.
 * Tests bypass this via the [engine] parameter on [HttpClientFactory.create].
 */
expect fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient
