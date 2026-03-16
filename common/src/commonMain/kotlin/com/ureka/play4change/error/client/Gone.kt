package com.ureka.play4change.error.client

sealed class Gone(messageKey: String, params: List<Any> = emptyList()) :
    ClientError(410, messageKey, params) {

    companion object{
        const val DELETED_KEY = "error.gone.resource_deleted"
    }
    data class ResourceDeleted(val resourceType: String) :
        Gone(DELETED_KEY, listOf(resourceType))
}