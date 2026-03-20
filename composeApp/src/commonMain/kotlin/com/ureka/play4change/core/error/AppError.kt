package com.ureka.play4change.core.error

sealed interface AppError {
    val messageKey: String
    val params: List<String>

    sealed interface ClientError : AppError {
        data object NetworkUnavailable : ClientError {
            override val messageKey = "error_network"
            override val params = emptyList<String>()
        }
        data class ValidationError(val field: String, override val messageKey: String) : ClientError {
            override val params = listOf(field)
        }
        data object Unauthorised : ClientError {
            override val messageKey = "error_auth_required"
            override val params = emptyList<String>()
        }
    }

    sealed interface ServerError : AppError {
        data object ServiceUnavailable : ServerError {
            override val messageKey = "error_service_unavailable"
            override val params = emptyList<String>()
        }
        data object NotFound : ServerError {
            override val messageKey = "error_not_found"
            override val params = emptyList<String>()
        }
        data class Unexpected(val technicalMessage: String?) : ServerError {
            override val messageKey = "error_unexpected"
            override val params = emptyList<String>()
        }
    }
}
