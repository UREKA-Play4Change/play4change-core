package com.ureka.play4change.error.client

sealed class UnprocessableEntity(messageKey: String, params: List<Any> = emptyList()) :
    ClientError(422, messageKey, params) {

    companion object{
        const val RULE_KEY = "error.unprocessable.business_rule"
        const val STATE_KEY = "error.unprocessable.invalid_state"
        const val VALUE_OBJECT_KEY = "error.unprocessable.value_object"
    }
    data class BusinessRuleViolation(val rule: String) :
        UnprocessableEntity(RULE_KEY, listOf(rule))

    data class InvalidState(val currentState: String, val expectedState: String) :
        UnprocessableEntity(
            STATE_KEY,
            listOf(currentState, expectedState)
        )

    data class ValueObjectInvalid(val type: String, val value: String) :
        UnprocessableEntity(VALUE_OBJECT_KEY, listOf(type, value))
}