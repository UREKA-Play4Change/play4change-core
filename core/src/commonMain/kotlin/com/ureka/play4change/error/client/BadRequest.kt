package com.ureka.play4change.error.client

sealed class BadRequest(messageKey: String, params: List<Any> = emptyList()) :
    ClientError(400, messageKey, params) {

    companion object{
        const val MISSING_KEY = "error.bad_request.missing_field"
        const val INVALID_KEY = "error.bad_request.invalid_field"
        const val MALFORMED_KEY = "error.bad_request.malformed_body"
        const val PAGINATION_KEY = "error.bad_request.invalid_pagination"
    }

    data class MissingField(val field: String) :
        BadRequest(MISSING_KEY, listOf(field))

    data class InvalidField(val field: String, val reason: String = "") :
        BadRequest(INVALID_KEY, listOf(field, reason))


    data object MalformedBody :
        BadRequest(MALFORMED_KEY)


    data class InvalidPagination(val detail: String = "") :
        BadRequest(PAGINATION_KEY, listOf(detail))
}