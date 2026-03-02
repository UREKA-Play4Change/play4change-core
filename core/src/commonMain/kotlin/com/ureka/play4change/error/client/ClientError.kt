package com.ureka.play4change.error.client

import com.ureka.play4change.error.AppError

sealed class ClientError(
    override val httpStatus: Int,
    override val messageKey: String,
    override val params: List<Any> = emptyList()
) : AppError{
    override fun toString(): String =
        "${this::class.simpleName}(httpStatus=$httpStatus, messageKey=$messageKey, params=$params)"
}