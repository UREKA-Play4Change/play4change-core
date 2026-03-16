package com.ureka.play4change.error.server

sealed class BadGateway(messageKey: String, params: List<Any> = emptyList()) :
    ServerError(502, messageKey, params) {

    companion object{
        const val DOWNSTREAM_KEY = "error.bad_gateway.downstream"
    }
    data class DownstreamServiceError(val service: String) :
        BadGateway(DOWNSTREAM_KEY, listOf(service))
}