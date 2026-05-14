package com.ureka.play4change.core.network

import com.ureka.play4change.core.error.AppError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

/**
 * Maps an HTTP status code to the corresponding [NetworkError].
 * Returns [NetworkError.Unknown] for codes that have no specific mapping.
 */
fun networkErrorFromStatus(code: Int): NetworkError = when (code) {
    401 -> NetworkError.Unauthorized
    403 -> NetworkError.Forbidden
    404 -> NetworkError.NotFound()
    in 500..599 -> NetworkError.ServerError(code)
    else -> NetworkError.Unknown("HTTP $code")
}

/**
 * Maps a [Throwable] to the most specific [NetworkError].
 * [NetworkException] instances are unwrapped; Ktor timeout exceptions are
 * mapped to [NetworkError.Timeout]; platform-detected no-connection errors
 * are mapped to [NetworkError.NoConnection]; all others fall back to [NetworkError.Unknown].
 */
fun Throwable.toNetworkError(): NetworkError = when (this) {
    is NetworkException -> this.error
    is HttpRequestTimeoutException -> NetworkError.Timeout
    is SocketTimeoutException -> NetworkError.Timeout
    is ConnectTimeoutException -> NetworkError.Timeout
    else -> {
        val className = this::class.simpleName ?: ""
        when {
            className.contains("ConnectTimeout") || className.contains("SocketTimeout") ->
                NetworkError.Timeout
            isNetworkUnavailableError(this) -> NetworkError.NoConnection
            else -> NetworkError.Unknown(this.message ?: "Unknown error")
        }
    }
}

/**
 * Maps a [NetworkError] to the corresponding [AppError] for use in MVI state.
 */
fun NetworkError.toAppError(): AppError = when (this) {
    NetworkError.Unauthorized -> AppError.ClientError.Unauthorised
    NetworkError.Forbidden -> AppError.ServerError.Unexpected("forbidden")
    is NetworkError.NotFound -> AppError.ServerError.NotFound
    is NetworkError.ServerError -> AppError.ServerError.ServiceUnavailable
    NetworkError.NoConnection -> AppError.ClientError.NetworkUnavailable
    NetworkError.Timeout -> AppError.ClientError.NetworkUnavailable
    is NetworkError.Unknown -> AppError.ServerError.Unexpected(this.message)
    NetworkError.TaskGenerationPending -> AppError.ServerError.ServiceUnavailable
    NetworkError.NoTaskAvailable -> AppError.ServerError.NotFound
}
