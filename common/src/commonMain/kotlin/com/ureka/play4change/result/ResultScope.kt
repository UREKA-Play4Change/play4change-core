package com.ureka.play4change.result

import com.ureka.play4change.error.AppError
import com.ureka.play4change.result.throwable.ResultControlFlowException

interface ResultScope {

    /**
     * Unwraps a [Result] or short-circuits the enclosing [resultWrapper] with the error.
     */
    fun <T, E : AppError> Result<T, E>.bind(): T = when (this) {
        is Result.Success -> data
        is Result.Failure -> throw ResultControlFlowException(error)
    }

    /**
     * Asserts a domain invariant.  If [condition] is false, short-circuits with [errorBuilder].
     */
    fun ensure(condition: Boolean, errorBuilder: () -> AppError) {
        if (!condition) throw ResultControlFlowException(errorBuilder())
    }

    /**
     * Asserts that [value] is not null, binding the non-null value or short-circuiting.
     */
    fun <T : Any> ensureNotNull(value: T?, errorBuilder: () -> AppError): T {
        return value ?: throw ResultControlFlowException(errorBuilder())
    }
}