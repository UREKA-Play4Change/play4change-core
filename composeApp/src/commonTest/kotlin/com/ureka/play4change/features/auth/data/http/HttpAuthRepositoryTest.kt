package com.ureka.play4change.features.auth.data.http

import com.ureka.play4change.core.network.HttpClientFactory
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.NetworkError
import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.TokenStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpAuthRepositoryTest {

    private val storage = FakeTokenStorage()

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private class FakeTokenStorage : TokenStorage {
        private var access: String? = null
        private var refresh: String? = null

        override suspend fun getAccessToken(): String? = access
        override suspend fun getRefreshToken(): String? = refresh
        override suspend fun store(accessToken: String, refreshToken: String) {
            access = accessToken; refresh = refreshToken
        }

        override suspend fun clear() {
            access = null; refresh = null
        }
    }

    /** Creates a minimal JWT whose payload contains {"sub":"<sub>"}. */
    @OptIn(ExperimentalEncodingApi::class)
    private fun fakeJwt(sub: String): String {
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val payloadJson = """{"sub":"$sub","exp":9999999999}"""
        val payload = Base64.UrlSafe.encode(payloadJson.encodeToByteArray()).trimEnd('=')
        return "$header.$payload.sig"
    }

    private fun tokenJson(sub: String) =
        """{"accessToken":"${fakeJwt(sub)}","refreshToken":"ref-$sub","expiresIn":900}"""

    private fun buildRepo(engine: MockEngine): HttpAuthRepository =
        HttpAuthRepository(
            client = HttpClientFactory.create(
                tokenStorage = storage,
                networkConfig = NetworkConfig("http://localhost"),
                onSessionExpired = {},
                engine = engine
            ),
            tokenStorage = storage
        )

    // ---------------------------------------------------------------------------
    // sendMagicLink
    // ---------------------------------------------------------------------------

    @Test
    fun `given valid email when sendMagicLink called then POST magic-link is requested and returns success`() =
        runTest {
            val engine = MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/auth/magic-link", request.url.encodedPath)
                respond(
                    content = ByteReadChannel("""{"message":"Magic link sent"}"""),
                    status = HttpStatusCode.Accepted,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val result = buildRepo(engine).sendMagicLink("user@example.com")

            assertTrue(result.success)
        }

    // ---------------------------------------------------------------------------
    // verifyMagicLink
    // ---------------------------------------------------------------------------

    @Test
    fun `given valid token when verifyMagicLink called then returns AuthResult and stores tokens`() =
        runTest {
            val engine = MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/auth/magic-link/verify", request.url.encodedPath)
                respond(
                    content = ByteReadChannel(tokenJson("user-abc")),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val result = buildRepo(engine).verifyMagicLink("good-token")

            assertNotNull(result)
            assertEquals("user-abc", result.userId)
            assertEquals("ref-user-abc", result.tokens.refreshToken)
            assertEquals(result.tokens.accessToken, storage.getAccessToken())
        }

    @Test
    fun `given expired token when verifyMagicLink called then throws NetworkException Unauthorized`() =
        runTest {
            val engine = MockEngine { _ ->
                respond(content = ByteReadChannel(""), status = HttpStatusCode.Unauthorized)
            }

            val ex = assertFailsWith<NetworkException> {
                buildRepo(engine).verifyMagicLink("expired-token")
            }
            assertIs<NetworkError.Unauthorized>(ex.error)
        }

    // ---------------------------------------------------------------------------
    // logout
    // ---------------------------------------------------------------------------

    @Test
    fun `when logout called then POST auth-logout is requested and token storage is cleared`() =
        runTest {
            storage.store("access-token", "refresh-token")
            var logoutCalled = false

            val engine = MockEngine { request ->
                logoutCalled = true
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/auth/logout", request.url.encodedPath)
                respond(content = ByteReadChannel(""), status = HttpStatusCode.NoContent)
            }

            buildRepo(engine).logout()

            assertTrue(logoutCalled)
            assertNull(storage.getAccessToken())
            assertNull(storage.getRefreshToken())
        }
}
