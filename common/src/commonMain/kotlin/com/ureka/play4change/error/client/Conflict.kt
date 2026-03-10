package com.ureka.play4change.error.client

sealed class Conflict(messageKey: String, params: List<Any> = emptyList()) :
    ClientError(409, messageKey, params) {

    companion object{
        const val DUPLICATE_KEY = "error.conflict.duplicate_resource"
        const val LOCK_KEY = "error.conflict.optimistic_lock"
        const val CONCURRENT_KEY = "error.conflict.concurrent_modification"
    }
    data class DuplicateResource(val resourceType: String, val field: String) :
        Conflict(DUPLICATE_KEY, listOf(resourceType, field))

    data class OptimisticLockViolation(val resourceType: String) :
        Conflict(LOCK_KEY, listOf(resourceType))

    data object ConcurrentModification :
        Conflict(CONCURRENT_KEY)
}