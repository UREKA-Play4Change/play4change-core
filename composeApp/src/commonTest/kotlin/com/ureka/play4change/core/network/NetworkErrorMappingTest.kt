package com.ureka.play4change.core.network

import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NetworkErrorMappingTest {

    // ---------------------------------------------------------------------------
    // networkErrorFromStatus — HTTP code → NetworkError
    // ---------------------------------------------------------------------------

    @Test
    fun `HTTP 401 maps to Unauthorized`() {
        assertEquals(NetworkError.Unauthorized, networkErrorFromStatus(401))
    }

    @Test
    fun `HTTP 403 maps to Forbidden`() {
        assertEquals(NetworkError.Forbidden, networkErrorFromStatus(403))
    }

    @Test
    fun `HTTP 404 maps to NotFound`() {
        assertIs<NetworkError.NotFound>(networkErrorFromStatus(404))
    }

    @Test
    fun `HTTP 500 maps to ServerError with code 500`() {
        val error = networkErrorFromStatus(500)
        assertIs<NetworkError.ServerError>(error)
        assertEquals(500, error.code)
    }

    @Test
    fun `HTTP 503 maps to ServerError with code 503`() {
        val error = networkErrorFromStatus(503)
        assertIs<NetworkError.ServerError>(error)
        assertEquals(503, error.code)
    }

    @Test
    fun `HTTP 200 maps to Unknown`() {
        assertIs<NetworkError.Unknown>(networkErrorFromStatus(200))
    }

    // ---------------------------------------------------------------------------
    // Throwable.toNetworkError — exception → NetworkError
    // ---------------------------------------------------------------------------

    @Test
    fun `NetworkException with NoConnection maps to NoConnection`() {
        val e = NetworkException(NetworkError.NoConnection)
        assertEquals(NetworkError.NoConnection, e.toNetworkError())
    }

    @Test
    fun `NetworkException with Timeout maps to Timeout`() {
        val e = NetworkException(NetworkError.Timeout)
        assertEquals(NetworkError.Timeout, e.toNetworkError())
    }

    @Test
    fun `NetworkException with Unauthorized maps to Unauthorized`() {
        val e = NetworkException(NetworkError.Unauthorized)
        assertEquals(NetworkError.Unauthorized, e.toNetworkError())
    }

    @Test
    fun `HttpRequestTimeoutException maps to Timeout`() {
        val e = HttpRequestTimeoutException("http://localhost/test", 5000L)
        assertEquals(NetworkError.Timeout, e.toNetworkError())
    }

    @Test
    fun `IOException maps to NoConnection`() {
        val e = java.io.IOException("Connection refused")
        assertEquals(NetworkError.NoConnection, e.toNetworkError())
    }

    @Test
    fun `Unknown exception maps to Unknown NetworkError`() {
        val e = RuntimeException("Something broke")
        assertIs<NetworkError.Unknown>(e.toNetworkError())
    }

    // ---------------------------------------------------------------------------
    // NetworkError.toAppError — NetworkError → AppError
    // ---------------------------------------------------------------------------

    @Test
    fun `Unauthorized toAppError maps to Unauthorised`() {
        assertIs<com.ureka.play4change.core.error.AppError.ClientError.Unauthorised>(
            NetworkError.Unauthorized.toAppError()
        )
    }

    @Test
    fun `NoConnection toAppError maps to NetworkUnavailable`() {
        assertIs<com.ureka.play4change.core.error.AppError.ClientError.NetworkUnavailable>(
            NetworkError.NoConnection.toAppError()
        )
    }

    @Test
    fun `Timeout toAppError maps to NetworkUnavailable`() {
        assertIs<com.ureka.play4change.core.error.AppError.ClientError.NetworkUnavailable>(
            NetworkError.Timeout.toAppError()
        )
    }

    @Test
    fun `ServerError toAppError maps to ServiceUnavailable`() {
        assertIs<com.ureka.play4change.core.error.AppError.ServerError.ServiceUnavailable>(
            NetworkError.ServerError(500).toAppError()
        )
    }

    // ---------------------------------------------------------------------------
    // HomeState and TaskState carry networkError field
    // ---------------------------------------------------------------------------

    @Test
    fun `HomeState networkError field is set to NoConnection`() {
        val state = com.ureka.play4change.features.home.presentation.HomeState(
            networkError = NetworkError.NoConnection
        )
        assertIs<NetworkError.NoConnection>(state.networkError)
    }

    @Test
    fun `TaskState networkError field is set to NoConnection`() {
        val state = com.ureka.play4change.features.task.presentation.TaskState(
            networkError = NetworkError.NoConnection
        )
        assertIs<NetworkError.NoConnection>(state.networkError)
    }

    @Test
    fun `HomeState with NoConnection networkError implies retry button should be visible`() {
        val state = com.ureka.play4change.features.home.presentation.HomeState(
            isLoading = false,
            networkError = NetworkError.NoConnection
        )
        // Retry button is shown when networkError is NoConnection or Timeout
        assertTrue(
            state.networkError is NetworkError.NoConnection || state.networkError is NetworkError.Timeout,
            "Expected retryable network error but got ${state.networkError}"
        )
    }
}
