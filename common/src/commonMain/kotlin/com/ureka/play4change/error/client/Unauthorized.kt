package com.ureka.play4change.error.client

import com.ureka.play4change.error.AppError

sealed class Unauthorized(
    override val httpStatus: Int = 401,
    override val messageKey: String,
    override val params: List<Any> = emptyList()
) : AppError {

    /** Magic link token not found, expired, or already used. */
    data object InvalidOrExpiredMagicLink : Unauthorized(
        messageKey = "error.auth.magic_link.invalid"
    )

    /** JWT access token is missing, malformed, or expired. */
    data object InvalidToken : Unauthorized(
        messageKey = "error.auth.token.invalid"
    )

    /** JWT access token has expired. Client should refresh. */
    data object TokenExpired : Unauthorized(
        messageKey = "error.auth.token.expired"
    )

    /** Refresh token not found. */
    data object RefreshTokenNotFound : Unauthorized(
        messageKey = "error.auth.refresh.not_found"
    )

    /** Refresh token already used — possible theft detected, all sessions revoked. */
    data object RefreshTokenReuse : Unauthorized(
        messageKey = "error.auth.refresh.reuse_detected"
    )

    /** Credentials are missing from the request. */
    data object MissingCredentials : Unauthorized(
        messageKey = "error.auth.credentials.missing"
    )
}
