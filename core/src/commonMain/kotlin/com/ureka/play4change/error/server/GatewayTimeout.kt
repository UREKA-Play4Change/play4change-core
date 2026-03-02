package com.ureka.play4change.error.server

sealed class GatewayTimeout(messageKey: String, params: List<Any> = emptyList()) :
    ServerError(504, messageKey, params) {

    companion object{
        const val TIMEOUT_KEY = "error.gateway_timeout.downstream"
    }
    data class DownstreamTimeout(val service: String) :
        GatewayTimeout(TIMEOUT_KEY, listOf(service))
}