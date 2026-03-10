package com.ureka.play4change.error.client

sealed class Forbidden(messageKey: String, params: List<Any> = emptyList()) :
    ClientError(403, messageKey, params) {

    companion object{
        const val ROLE_KEY = "error.forbidden.insufficient_role"
        const val OWNERSHIP_KEY = "error.forbidden.resource_ownership"
        const val REVOKED_KEY = "error.forbidden.access_revoked"
    }

    data class InsufficientRole(val requiredRole: String) :
        Forbidden(ROLE_KEY, listOf(requiredRole))

    data class ResourceOwnershipViolation(val resourceType: String) :
        Forbidden(OWNERSHIP_KEY, listOf(resourceType))

    data object AccessRevoked :
        Forbidden(REVOKED_KEY)
}