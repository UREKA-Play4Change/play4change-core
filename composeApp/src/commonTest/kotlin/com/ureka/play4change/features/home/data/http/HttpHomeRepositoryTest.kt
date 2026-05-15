package com.ureka.play4change.features.home.data.http

import com.ureka.play4change.core.network.HttpClientFactory
import com.ureka.play4change.core.network.NetworkConfig
import com.ureka.play4change.core.network.TokenStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpHomeRepositoryTest {

    private val storage = FakeTokenStorage()

    private class FakeTokenStorage : TokenStorage {
        override suspend fun getAccessToken(): String? = null
        override suspend fun getRefreshToken(): String? = null
        override suspend fun store(accessToken: String, refreshToken: String) = Unit
        override suspend fun clear() = Unit
    }

    private val profileJson = """
        {"name":"Radesh","streakDays":5,"totalPoints":500,"level":2,"currentDay":3,"totalDays":14}
    """.trimIndent()

    private fun buildRepo(engine: MockEngine): HttpHomeRepository =
        HttpHomeRepository(
            client = HttpClientFactory.create(
                tokenStorage = storage,
                networkConfig = NetworkConfig("http://localhost"),
                onSessionExpired = {},
                engine = engine
            ),
            tokenStorage = storage
        )

    // ---------------------------------------------------------------------------
    // isEnrolled flag
    // ---------------------------------------------------------------------------

    @Test
    fun `getHomeData returns isEnrolled=false when no topic has isEnrolled=true`() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/profile" -> respond(
                    content = ByteReadChannel(profileJson),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/topics" -> respond(
                    content = ByteReadChannel("""[{"id":"t1","isEnrolled":false}]"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
            }
        }

        val data = buildRepo(engine).getHomeData("user-1")

        assertFalse(data.isEnrolled)
        assertTrue(data.todayTasks.isEmpty())
    }

    @Test
    fun `getHomeData returns isEnrolled=true when at least one topic has isEnrolled=true`() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/profile" -> respond(
                    content = ByteReadChannel(profileJson),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/topics" -> respond(
                    content = ByteReadChannel("""[{"id":"t1","title":"Sustainability","isEnrolled":true}]"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/tasks/today" -> respond(
                    content = ByteReadChannel("""{"assignmentId":"a1","title":"Quiz","pointsReward":50}"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
            }
        }

        val data = buildRepo(engine).getHomeData("user-1")

        assertTrue(data.isEnrolled)
    }

    // ---------------------------------------------------------------------------
    // todayTasks — multi-topic
    // ---------------------------------------------------------------------------

    @Test
    fun `getHomeData returns one TaskSummaryWithTopic per enrolled topic`() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/profile" -> respond(
                    content = ByteReadChannel(profileJson),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath == "/topics" -> respond(
                    content = ByteReadChannel(
                        """[
                            {"id":"sustainability","title":"Sustainability","isEnrolled":true},
                            {"id":"digital","title":"Digital","isEnrolled":true},
                            {"id":"health","title":"Health","isEnrolled":false}
                        ]"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath == "/tasks/today" && request.url.parameters["topicId"] == "sustainability" -> respond(
                    content = ByteReadChannel("""{"assignmentId":"a1","title":"Sustainability Quiz","pointsReward":50}"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath == "/tasks/today" && request.url.parameters["topicId"] == "digital" -> respond(
                    content = ByteReadChannel("""{"assignmentId":"a2","title":"Digital Quiz","pointsReward":40}"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
            }
        }

        val data = buildRepo(engine).getHomeData("user-1")

        assertEquals(2, data.todayTasks.size)
        val sustainability = data.todayTasks.first { it.topicId == "sustainability" }
        assertEquals("Sustainability", sustainability.topicTitle)
        assertNotNull(sustainability.task)
        assertFalse(sustainability.completed)

        val digital = data.todayTasks.first { it.topicId == "digital" }
        assertEquals("Digital", digital.topicTitle)
        assertNotNull(digital.task)
        assertFalse(digital.completed)
    }

    @Test
    fun `getHomeData marks entry as completed when tasks today returns 404`() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/profile" -> respond(
                    content = ByteReadChannel(profileJson),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/topics" -> respond(
                    content = ByteReadChannel("""[{"id":"t1","title":"Sustainability","isEnrolled":true}]"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/tasks/today" -> respond(
                    content = ByteReadChannel(""),
                    status = HttpStatusCode.NotFound
                )
                else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
            }
        }

        val data = buildRepo(engine).getHomeData("user-1")

        assertEquals(1, data.todayTasks.size)
        val entry = data.todayTasks.first()
        assertTrue(entry.completed)
        assertNull(entry.task)
    }

    // ---------------------------------------------------------------------------
    // pendingReviews
    // ---------------------------------------------------------------------------

    @Test
    fun `getHomeData returns pending reviews from GET reviews pending`() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/profile" -> respond(
                    content = ByteReadChannel(profileJson),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath == "/topics" -> respond(
                    content = ByteReadChannel("""[{"id":"t1","title":"Sustainability","isEnrolled":true}]"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                request.url.encodedPath == "/tasks/today" -> respond(
                    content = ByteReadChannel(""),
                    status = HttpStatusCode.NotFound
                )
                request.url.encodedPath == "/reviews/pending" -> respond(
                    content = ByteReadChannel("""[{"reviewId":"r1","submissionPhotoUrl":"https://example.com/photo.jpg"}]"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
            }
        }

        val data = buildRepo(engine).getHomeData("user-1")

        assertEquals(1, data.pendingReviews.size)
        val review = data.pendingReviews.first()
        assertEquals("r1", review.reviewId)
        assertEquals("Sustainability", review.topicTitle)
        assertEquals("https://example.com/photo.jpg", review.photoUrl)
    }

    @Test
    fun `getHomeData returns empty pendingReviews when reviews endpoint returns empty list`() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/profile" -> respond(
                    content = ByteReadChannel(profileJson),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/topics" -> respond(
                    content = ByteReadChannel("""[{"id":"t1","title":"Sustainability","isEnrolled":true}]"""),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/tasks/today" -> respond(
                    content = ByteReadChannel(""),
                    status = HttpStatusCode.NotFound
                )
                "/reviews/pending" -> respond(
                    content = ByteReadChannel("[]"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
            }
        }

        val data = buildRepo(engine).getHomeData("user-1")

        assertTrue(data.pendingReviews.isEmpty())
    }
}
