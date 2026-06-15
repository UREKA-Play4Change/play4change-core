package com.ureka.play4change.core.network

/**
 * Typed network error surface used by all HTTP repository implementations.
 * Error states must be mapped to this sealed class before reaching the
 * presentation layer — raw HTTP codes must not leak upwards.
 */
sealed class NetworkError {
    /** 401 — access token expired or revoked; the Auth plugin could not refresh. */
    data object Unauthorized : NetworkError()

    /** 403 — authenticated but insufficient role for this operation. */
    data object Forbidden : NetworkError()

    /** 404 — resource not found. [id] identifies which resource was missing. */
    data class NotFound(val id: String = "") : NetworkError()

    /** 5xx — server-side failure. */
    data class ServerError(val code: Int) : NetworkError()

    /** Network unavailable or connection refused. */
    data object NoConnection : NetworkError()

    /** Request or socket timed out. */
    data object Timeout : NetworkError()

    /** 429 — too many requests; the server-side rate limit for this endpoint was exceeded. */
    data object RateLimited : NetworkError()

    /** Any other error not covered by the cases above. */
    data class Unknown(val message: String) : NetworkError()

    /** 202 — topic content generation is still in progress; no task is ready yet. */
    data object TaskGenerationPending : NetworkError()

    /** 404 on GET /tasks/today — the user has no task available today (already completed or not yet due). */
    data object NoTaskAvailable : NetworkError()

    /** 409 on GET /tasks/today — an open struggle session exists; route to it instead of showing a task. */
    data class StruggleOpen(val enrollmentId: String) : NetworkError()
}
