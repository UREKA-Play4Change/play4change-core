package com.ureka.play4change.error.server

sealed class ServiceUnavailable(messageKey: String, params: List<Any> = emptyList()) :
    ServerError(503, messageKey, params) {

    companion object{
        const val MAINTENANCE_KEY = "error.service_unavailable.maintenance"
        const val UNAVAILABLE_KEY = "error.service_unavailable.dependency"
    }

    data object Maintenance :
        ServiceUnavailable(MAINTENANCE_KEY)

    data class DependencyUnavailable(val dependency: String) :
        ServiceUnavailable(UNAVAILABLE_KEY, listOf(dependency))
}