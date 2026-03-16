package com.ureka.play4change.error.server

sealed class InternalServerError(messageKey: String) :
    ServerError(500, messageKey) {

    companion object{
        const val UNEXPECTED_KEY = "error.internal.unexpected"
        const val VIOLATION_KEY = "error.internal.service_contract"
    }

    data object UnexpectedException :
        InternalServerError(UNEXPECTED_KEY)

    data object ServiceContractViolation :
        InternalServerError(VIOLATION_KEY)
}