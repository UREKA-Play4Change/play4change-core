package com.ureka.play4change.features.profile.data.http

import com.ureka.play4change.core.network.HttpClientFactory
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.TokenStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpProfileRepositoryTest {

    private class FakeTokenStorage(private var refresh: String? = "refresh-tok") : TokenStorage {
        var cleared = false
        override suspend fun getAccessToken(): String? = null
        override suspend fun getRefreshToken(): String? = refresh
        override suspend fun store(accessToken: String, refreshToken: String) = Unit
        override suspend fun clear() { cleared = true }
    }

    private fun buildRepo(engine: MockEngine, tokenStorage: TokenStorage = FakeTokenStorage()): HttpProfileRepository =
        HttpProfileRepository(
            client = HttpClientFactory.create(
                tokenStorage = tokenStorage,
                networkConfig = NetworkConfig("http://localhost"),
                onSessionExpired = {},
                engine = engine
            ),
            tokenStorage = tokenStorage
        )

    // ---------------------------------------------------------------------------
    // getProfile
    // ---------------------------------------------------------------------------

    @Test
    fun `getProfile calls GET profile and returns ProfileData with correct fields`() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/profile" -> respond(
                    content = ByteReadChannel(
                        """{"userId":"u-1","name":"Alice","email":"alice@example.com","streakDays":5,"totalPoints":320,"accuracy":0.8,"level":4,"currentDay":3,"totalDays":9}"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/profile/badges" -> respond(
                    content = ByteReadChannel("[]"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(ByteReadChannel(""), HttpStatusCode.NotFound)
            }
        }

        val profile = buildRepo(engine).getProfile("ignored")

        assertEquals("u-1", profile.userId)
        assertEquals("Alice", profile.name)
        assertEquals("alice@example.com", profile.email)
        assertEquals(5, profile.streakDays)
        assertEquals(320, profile.totalPoints)
        assertEquals(0.8f, profile.accuracy)
        assertTrue(profile.badges.isEmpty())
    }

    @Test
    fun `getProfile calls GET profile badges and maps to Badge list with correct fields`() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/profile" -> respond(
                    content = ByteReadChannel(
                        """{"userId":"u-1","name":"Alice","email":"alice@example.com","streakDays":0,"totalPoints":0,"accuracy":0.0,"level":1,"currentDay":0,"totalDays":0}"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/profile/badges" -> respond(
                    content = ByteReadChannel(
                        """[{"microCompetenceName":"first_task","description":"Complete your first task","topicTitle":"Sustainability","earnedAt":"2025-01-15T10:30:00Z"}]"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/topics" -> respond(
                    content = ByteReadChannel(
                        """[{"id":"topic-sust","title":"Sustainability","enrollmentStatus":"ACTIVE"}]"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(ByteReadChannel(""), HttpStatusCode.NotFound)
            }
        }

        val profile = buildRepo(engine).getProfile("ignored")

        assertEquals(1, profile.badges.size)
        val badge = profile.badges[0]
        assertEquals("topic-sust", badge.id)
        assertEquals("Sustainability", badge.titleKey)
        assertTrue(badge.isUnlocked)
        // earnedAt "2025-01-15T10:30:00Z" should parse to a positive epoch millis
        assertTrue((badge.unlockedAt ?: 0L) > 0L)
    }

    @Test
    fun `getProfile uses GET method for profile and badges and topics endpoints`() = runTest {
        val methods = mutableListOf<HttpMethod>()
        val engine = MockEngine { request ->
            methods += request.method
            when (request.url.encodedPath) {
                "/profile" -> respond(
                    content = ByteReadChannel(
                        """{"userId":"u-1","name":"Bob","email":"bob@example.com","streakDays":0,"totalPoints":0,"accuracy":0.0,"level":1,"currentDay":0,"totalDays":0}"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/profile/badges" -> respond(
                    content = ByteReadChannel("[]"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/topics" -> respond(
                    content = ByteReadChannel("[]"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(ByteReadChannel(""), HttpStatusCode.NotFound)
            }
        }

        buildRepo(engine).getProfile("ignored")

        assertTrue(methods.all { it == HttpMethod.Get })
        assertEquals(3, methods.size)
    }

    // ---------------------------------------------------------------------------
    // signOut
    // ---------------------------------------------------------------------------

    @Test
    fun `signOut clears token storage`() = runTest {
        val tokenStorage = FakeTokenStorage(refresh = "tok-123")
        val engine = MockEngine { _ ->
            respond(ByteReadChannel(""), HttpStatusCode.NoContent)
        }

        buildRepo(engine, tokenStorage).signOut()

        assertTrue(tokenStorage.cleared)
    }
}
