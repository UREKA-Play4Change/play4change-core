package com.ureka.play4change.features.struggle.data.http

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpStruggleRepositoryTest {

    private class FakeTokenStorage : TokenStorage {
        override suspend fun getAccessToken(): String? = null
        override suspend fun getRefreshToken(): String? = null
        override suspend fun store(accessToken: String, refreshToken: String) = Unit
        override suspend fun clear() = Unit
    }

    private fun buildRepo(engine: MockEngine): HttpStruggleRepository =
        HttpStruggleRepository(
            client = HttpClientFactory.create(
                tokenStorage = FakeTokenStorage(),
                networkConfig = NetworkConfig("http://localhost"),
                onSessionExpired = {},
                engine = engine
            )
        )

    // ---------------------------------------------------------------------------
    // getSession
    // ---------------------------------------------------------------------------

    @Test
    fun `getSession calls GET struggle enrollment and returns StruggleSession on 200`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/struggle/enrollment/enroll-abc", request.url.encodedPath)
            respond(
                content = ByteReadChannel(
                    """{"sessionId":"sess-001","errorPattern":"WRONG_ANSWER","status":"ACTIVE","adaptiveTasks":[{"taskId":"task-001","title":"Recycling","description":"Which bin?","hint":"Think materials.","options":["A","B"],"pointsReward":50}]}"""
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val session = buildRepo(engine).getSession("enroll-abc")

        assertNotNull(session)
        assertEquals("sess-001", session.sessionId)
        assertEquals("WRONG_ANSWER", session.errorPattern)
        assertEquals(1, session.tasks.size)
        assertEquals("task-001", session.tasks[0].taskId)
        assertEquals("Think materials.", session.tasks[0].hint)
    }

    @Test
    fun `getSession returns null when server responds 404`() = runTest {
        val engine = MockEngine { _ ->
            respond(content = ByteReadChannel(""), status = HttpStatusCode.NotFound)
        }

        val session = buildRepo(engine).getSession("enroll-no-session")

        assertNull(session)
    }

    // ---------------------------------------------------------------------------
    // submitTask
    // ---------------------------------------------------------------------------

    @Test
    fun `submitTask calls POST struggle sessionId tasks taskId submit and returns result`() =
        runTest {
            val engine = MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals(
                    "/struggle/sess-001/tasks/task-001/submit",
                    request.url.encodedPath
                )
                respond(
                    content = ByteReadChannel(
                        """{"isCorrect":true,"pointsAwarded":50,"sessionResolved":false}"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val result = buildRepo(engine).submitTask("sess-001", "task-001", 0)

            assertTrue(result.isCorrect)
            assertEquals(50, result.pointsAwarded)
            assertFalse(result.sessionResolved)
        }

    @Test
    fun `submitTask with sessionResolved true reports session is done`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(
                    """{"isCorrect":true,"pointsAwarded":50,"sessionResolved":true}"""
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = buildRepo(engine).submitTask("sess-001", "task-001", 0)

        assertTrue(result.sessionResolved)
    }
}
