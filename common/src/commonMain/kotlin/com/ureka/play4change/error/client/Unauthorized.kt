package com.ureka.play4change.error.client

sealed class Unauthorized(messageKey: String) : ClientError(401, messageKey) {

    companion object{
        const val MISSING_KEY = "error.unauthorized.missing_credentials"
        const val EXPIRED_KEY = "error.unauthorized.token_expired"
        const val INVALID_KEY = "error.unauthorized.invalid_token"
    }

    data object MissingCredentials :
        Unauthorized(MISSING_KEY)

    data object TokenExpired :
        Unauthorized(EXPIRED_KEY)

    data object InvalidToken :
        Unauthorized(INVALID_KEY)
}