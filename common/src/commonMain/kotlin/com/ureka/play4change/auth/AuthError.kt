package com.ureka.play4change.auth

import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.Unauthorized

/**
 * Convenience typealias — auth operations return AppResult<AuthTokens>.
 * Failure errors are always Unauthorized subtypes.
 * Used by both the server application layer and the KMP client repository layer.
 */
typealias AuthResult<T> = com.ureka.play4change.result.Result<T, AppError>

/**
 * Maps the Unauthorized subtypes thrown by server application services
 * to their corresponding AppError. Used in the exception handler adapter.
 */
fun authErrorFromMessage(message: String?): Unauthorized = when {
    message?.contains("reuse") == true -> Unauthorized.RefreshTokenReuse
    message?.contains("expired") == true -> Unauthorized.TokenExpired
    message?.contains("not found") == true -> Unauthorized.RefreshTokenNotFound
    message?.contains("magic link") == true -> Unauthorized.InvalidOrExpiredMagicLink
    else -> Unauthorized.InvalidToken
}
