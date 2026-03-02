package com.ureka.play4change.error.client

sealed class NotFound(messageKey: String, params: List<Any> = emptyList()) :
    ClientError(404, messageKey, params) {

    companion object{
        const val RESOURCE_KEY = "error.not_found.resource"
        const val ROUTE_KEY = "error.not_found.route"
    }

    data class ResourceNotFound(val resourceType: String, val id: Any) :
        NotFound(RESOURCE_KEY, listOf(resourceType, id))

    data object RouteNotFound :
        NotFound(ROUTE_KEY)
}