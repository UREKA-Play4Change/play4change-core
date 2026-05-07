package com.ureka.play4change.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpClientRefreshTest {

    private class FakeTokenStorage : TokenStorage {
        private var accessToken: String? = null
        private var refreshToken: String? = null

        override suspend fun getAccessToken(): String? = accessToken
        override suspend fun getRefreshToken(): String? = refreshToken

        override suspend fun store(accessToken: String, refreshToken: String) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
        }

        override suspend fun clear() {
            accessToken = null
            refreshToken = null
        }
    }

    @Test
    fun `given 401 response when request made then refresh endpoint is called`() = runTest {
        var refreshCalled = false
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/refresh" -> {
                    refreshCalled = true
                    respond(
                        content = ByteReadChannel(
                            """{"accessToken":"new-access","refreshToken":"new-refresh","expiresIn":900}"""
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.Unauthorized)
            }
        }

        val storage = FakeTokenStorage().also { it.store("old-access", "old-refresh") }
        val client = HttpClientFactory.create(
            tokenStorage = storage,
            networkConfig = NetworkConfig("http://localhost"),
            onSessionExpired = {},
            engine = engine
        )

        client.get("/protected")

        assertTrue(refreshCalled, "Refresh endpoint must be called on 401")
    }

    @Test
    fun `given 401 and successful refresh when original request retried then uses new access token`() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/refresh" -> respond(
                    content = ByteReadChannel(
                        """{"accessToken":"new-access","refreshToken":"new-refresh","expiresIn":900}"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> {
                    val auth = request.headers[HttpHeaders.Authorization]
                    if (auth?.contains("new-access") == true) {
                        respond(content = ByteReadChannel("ok"), status = HttpStatusCode.OK)
                    } else {
                        respond(content = ByteReadChannel(""), status = HttpStatusCode.Unauthorized)
                    }
                }
            }
        }

        val storage = FakeTokenStorage().also { it.store("old-access", "old-refresh") }
        val client = HttpClientFactory.create(
            tokenStorage = storage,
            networkConfig = NetworkConfig("http://localhost"),
            onSessionExpired = {},
            engine = engine
        )

        val response = client.get("/protected")

        assertEquals(HttpStatusCode.OK, response.status, "Retry with new token must succeed")
        assertEquals("new-access", storage.getAccessToken(), "Storage must hold new access token")
    }

    @Test
    fun `given failed refresh when request made then session expired callback is invoked and storage is cleared`() =
        runTest {
            var sessionExpiredCalled = false
            val engine = MockEngine { _ ->
                respond(content = ByteReadChannel(""), status = HttpStatusCode.Unauthorized)
            }

            val storage = FakeTokenStorage().also { it.store("old-access", "old-refresh") }
            val client = HttpClientFactory.create(
                tokenStorage = storage,
                networkConfig = NetworkConfig("http://localhost"),
                onSessionExpired = { sessionExpiredCalled = true },
                engine = engine
            )

            client.get("/protected")

            assertTrue(sessionExpiredCalled, "onSessionExpired must be called when refresh fails")
            assertNull(storage.getAccessToken(), "Access token must be cleared after failed refresh")
            assertNull(storage.getRefreshToken(), "Refresh token must be cleared after failed refresh")
        }
}
