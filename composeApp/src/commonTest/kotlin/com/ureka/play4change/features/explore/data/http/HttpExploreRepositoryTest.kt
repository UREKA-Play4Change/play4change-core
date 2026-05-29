package com.ureka.play4change.features.explore.data.http

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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpExploreRepositoryTest {

    private val storage = FakeTokenStorage()

    private class FakeTokenStorage : TokenStorage {
        override suspend fun getAccessToken(): String? = null
        override suspend fun getRefreshToken(): String? = null
        override suspend fun store(accessToken: String, refreshToken: String) = Unit
        override suspend fun clear() = Unit
    }

    private fun buildRepo(engine: MockEngine): HttpExploreRepository =
        HttpExploreRepository(
            client = HttpClientFactory.create(
                tokenStorage = storage,
                networkConfig = NetworkConfig("http://localhost"),
                onSessionExpired = {},
                engine = engine
            )
        )

    // ---------------------------------------------------------------------------
    // getTopics
    // ---------------------------------------------------------------------------

    @Test
    fun `getTopics calls GET topics and returns mapped list`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/topics", request.url.encodedPath)
            respond(
                content = ByteReadChannel(
                    """[
                        {"id":"sustainability","title":"Sustainability","description":"Learn about climate.","category":"SUSTAINABILITY","taskCount":14,"isEnrolled":true,"enrollmentStatus":"ACTIVE"},
                        {"id":"digital","title":"Digital Literacy","description":"Navigate digital world.","category":"DIGITAL","taskCount":10,"isEnrolled":false}
                    ]"""
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val topics = buildRepo(engine).getTopics("user-1")

        assertEquals(2, topics.size)
        assertEquals("sustainability", topics[0].id)
        assertEquals("Sustainability", topics[0].title)
        assertTrue(topics[0].isActive)
        assertFalse(topics[1].isActive)
    }

    @Test
    fun `getTopics maps category to TopicIconType`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(
                    """[{"id":"t1","title":"Health","description":"Health topics.","category":"HEALTH","taskCount":5,"isEnrolled":false}]"""
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val topics = buildRepo(engine).getTopics("user-1")

        assertEquals(1, topics.size)
        assertEquals(
            com.ureka.play4change.features.explore.domain.model.TopicIconType.HEALTH,
            topics[0].iconType
        )
    }

    // ---------------------------------------------------------------------------
    // enrollTopic
    // ---------------------------------------------------------------------------

    @Test
    fun `enrollTopic calls POST topics topicId enroll and returns true on 201`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/topics/digital/enroll", request.url.encodedPath)
            respond(
                content = ByteReadChannel(
                    """{"id":"enroll-1","topicId":"digital","status":"ACTIVE","currentDayIndex":0,"totalPointsEarned":0,"streakDays":0,"enrolledAt":"2026-05-07T10:00:00Z"}"""
                ),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = buildRepo(engine).enrollTopic("user-1", "digital")

        assertTrue(result)
    }

    @Test
    fun `enrollTopic returns false on server error`() = runTest {
        val engine = MockEngine { _ ->
            respond(content = ByteReadChannel(""), status = HttpStatusCode.InternalServerError)
        }

        val result = buildRepo(engine).enrollTopic("user-1", "unknown-topic")

        assertFalse(result)
    }
}
