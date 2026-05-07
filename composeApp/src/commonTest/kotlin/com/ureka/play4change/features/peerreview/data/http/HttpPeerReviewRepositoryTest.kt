package com.ureka.play4change.features.peerreview.data.http

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

class HttpPeerReviewRepositoryTest {

    private class FakeTokenStorage : TokenStorage {
        override suspend fun getAccessToken(): String? = null
        override suspend fun getRefreshToken(): String? = null
        override suspend fun store(accessToken: String, refreshToken: String) = Unit
        override suspend fun clear() = Unit
    }

    private fun buildRepo(engine: MockEngine): HttpPeerReviewRepository =
        HttpPeerReviewRepository(
            client = HttpClientFactory.create(
                tokenStorage = FakeTokenStorage(),
                networkConfig = NetworkConfig("http://localhost"),
                onSessionExpired = {},
                engine = engine
            )
        )

    // ---------------------------------------------------------------------------
    // getPendingReviews
    // ---------------------------------------------------------------------------

    @Test
    fun `getPendingReviews calls GET reviews pending with topicId and deserialises list`() =
        runTest {
            val engine = MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/reviews/pending", request.url.encodedPath)
                assertEquals("sustainability", request.url.parameters["topicId"])
                respond(
                    content = ByteReadChannel(
                        """[{"reviewId":"rev-001","submissionPhotoUrl":"https://example.com/photo.jpg"},{"reviewId":"rev-002","submissionPhotoUrl":null}]"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val reviews = buildRepo(engine).getPendingReviews("sustainability")

            assertEquals(2, reviews.size)
            assertEquals("rev-001", reviews[0].reviewId)
            assertEquals("https://example.com/photo.jpg", reviews[0].photoUrl)
            assertEquals("rev-002", reviews[1].reviewId)
            assertNull(reviews[1].photoUrl)
        }

    @Test
    fun `getPendingReviews returns empty list when server returns empty array`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("[]"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val reviews = buildRepo(engine).getPendingReviews("some-topic")

        assertTrue(reviews.isEmpty())
    }

    // ---------------------------------------------------------------------------
    // submitVerdict
    // ---------------------------------------------------------------------------

    @Test
    fun `submitVerdict with CORRECT calls POST reviews reviewId verdict and returns result`() =
        runTest {
            val engine = MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/reviews/rev-001/verdict", request.url.encodedPath)
                respond(
                    content = ByteReadChannel(
                        """{"verdict":"CORRECT","currentVerdicts":{"correct":1,"incorrect":0,"total":1},"finalized":true,"submitterPointsAwarded":50}"""
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val result = buildRepo(engine).submitVerdict("rev-001", "CORRECT", null)

            assertEquals("CORRECT", result.verdict)
            assertTrue(result.finalized)
            assertEquals(50, result.pointsAwarded)
        }

    @Test
    fun `submitVerdict with INCORRECT returns finalized false and no points`() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(
                    """{"verdict":"INCORRECT","currentVerdicts":{"correct":0,"incorrect":1,"total":1},"finalized":false,"submitterPointsAwarded":null}"""
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val result = buildRepo(engine).submitVerdict("rev-001", "INCORRECT", "Blurry photo")

        assertEquals("INCORRECT", result.verdict)
        assertNull(result.pointsAwarded)
    }
}
