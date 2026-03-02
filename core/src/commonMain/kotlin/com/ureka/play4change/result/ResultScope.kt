package com.ureka.play4change.result

import com.ureka.play4change.error.AppError
import com.ureka.play4change.result.throwable.ResultControlFlowException

interface ResultScope {
    fun <T, E : AppError> Result<T, E>.bind(): T {
        return when (this) {
            is Result.Success -> this.data
            is Result.Error -> throw ResultControlFlowException(this.error)
        }
    }

    fun ensure(condition: Boolean, errorBuilder: () -> AppError) {
        if (!condition) {
            throw ResultControlFlowException(errorBuilder())
        }
    }
}