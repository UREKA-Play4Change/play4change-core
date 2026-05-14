package com.ureka.play4change.features.task.data.http

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
import com.ureka.play4change.core.network.NetworkError
import com.ureka.play4change.core.network.NetworkException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HttpTaskRepositoryTest {

    private val storage = FakeTokenStorage()

    private class FakeTokenStorage : TokenStorage {
        override suspend fun getAccessToken(): String? = null
        override suspend fun getRefreshToken(): String? = null
        override suspend fun store(accessToken: String, refreshToken: String) = Unit
        override suspend fun clear() = Unit
    }

    private fun buildRepo(engine: MockEngine): HttpTaskRepository =
        HttpTaskRepository(
            client = HttpClientFactory.create(
                tokenStorage = storage,
                networkConfig = NetworkConfig("http://localhost"),
                onSessionExpired = {},
                engine = engine
            )
        )

    // ---------------------------------------------------------------------------
    // getTask
    // ---------------------------------------------------------------------------

    @Test
    fun `getTask calls GET tasks today with topicId and returns TaskDetail with assignmentId`() =
        runTest {
            val engine = MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/tasks/today", request.url.encodedPath)
                assertEquals("sustainability", request.url.parameters["topicId"])
                respond(
                    content = ByteReadChannel(
                        """{"assignmentId":"assign-abc","title":"Recycling Check","description":"Test your knowledge.","hint":"Think carefully.","options":["A","B","C","D"],"pointsReward":100,"dueAt":"2026-05-08T12:00:00Z","wrongAttemptCount":0}"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val task = buildRepo(engine).getTask("sustainability")

            assertEquals("assign-abc", task.userTaskId)
            assertEquals("Recycling Check", task.title)
            assertEquals("Test your knowledge.", task.description)
            assertEquals("Think carefully.", task.hint)
            assertEquals(listOf("A", "B", "C", "D"), task.options)
            assertEquals(100, task.pointsReward)
        }

    @Test
    fun `getTask with null hint maps to empty string`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(
                    """{"assignmentId":"assign-xyz","title":"Task","description":"Desc.","hint":null,"options":[],"pointsReward":50,"dueAt":"2026-05-08T12:00:00Z","wrongAttemptCount":0}"""
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val task = buildRepo(engine).getTask("some-topic")

        assertEquals("assign-xyz", task.userTaskId)
        assertEquals("", task.hint)
    }

    // ---------------------------------------------------------------------------
    // submitAnswer
    // ---------------------------------------------------------------------------

    @Test
    fun `submitAnswer calls POST tasks assignmentId submit and returns correct SubmitResult`() =
        runTest {
            val engine = MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/tasks/assign-abc/submit", request.url.encodedPath)
                respond(
                    content = ByteReadChannel(
                        """{"isCorrect":true,"pointsAwarded":100,"totalPoints":500,"streakDays":3,"struggleTriggered":false}"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val result = buildRepo(engine).submitAnswer("assign-abc", 0)

            assertTrue(result.isCorrect)
            assertEquals(100, result.pointsAwarded)
        }

    @Test
    fun `submitAnswer with wrong answer returns isCorrect false`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(
                    """{"isCorrect":false,"pointsAwarded":0,"totalPoints":400,"streakDays":2,"struggleTriggered":true}"""
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = buildRepo(engine).submitAnswer("assign-abc", 2)

        assertFalse(result.isCorrect)
        assertEquals(0, result.pointsAwarded)
    }

    // ---------------------------------------------------------------------------
    // getTask — non-200 status handling (Bug 1, issue #71)
    // ---------------------------------------------------------------------------

    @Test
    fun `getTask throws NetworkException with TaskGenerationPending when server responds 202`() =
        runTest {
            val engine = MockEngine { _ ->
                respond(
                    content = ByteReadChannel(""),
                    status = HttpStatusCode.Accepted,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val ex = assertFailsWith<NetworkException> {
                buildRepo(engine).getTask("sustainability")
            }
            assertIs<NetworkError.TaskGenerationPending>(ex.error)
        }

    @Test
    fun `getTask throws NetworkException with NoTaskAvailable when server responds 404`() =
        runTest {
            val engine = MockEngine { _ ->
                respond(
                    content = ByteReadChannel(""),
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val ex = assertFailsWith<NetworkException> {
                buildRepo(engine).getTask("sustainability")
            }
            assertIs<NetworkError.NoTaskAvailable>(ex.error)
        }

    @Test
    fun `getTask throws NetworkException with ServerError when server responds 500`() =
        runTest {
            val engine = MockEngine { _ ->
                respond(
                    content = ByteReadChannel(""),
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val ex = assertFailsWith<NetworkException> {
                buildRepo(engine).getTask("sustainability")
            }
            assertIs<NetworkError.ServerError>(ex.error)
        }

    @Test
    fun `getTask maps dueAt and wrongAttemptCount from server response`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(
                    """{"assignmentId":"assign-abc","title":"T","description":"D","hint":null,"options":[],"pointsReward":50,"dueAt":"2026-05-08T12:00:00Z","wrongAttemptCount":1}"""
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val task = buildRepo(engine).getTask("some-topic")

        assertEquals("2026-05-08T12:00:00Z", task.dueAt)
        assertEquals(1, task.wrongAttemptCount)
    }
}
