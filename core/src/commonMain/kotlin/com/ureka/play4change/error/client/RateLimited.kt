package com.ureka.play4change.error.client

sealed class RateLimited(messageKey: String, params: List<Any> = emptyList()) :
    ClientError(429, messageKey, params) {

    companion object{
        const val TOO_MANY_KEY = "error.rate_limited.too_many_requests"
    }
    data class TooManyRequests(val retryAfterSeconds: Long = 60) :
        RateLimited(TOO_MANY_KEY, listOf(retryAfterSeconds))
}