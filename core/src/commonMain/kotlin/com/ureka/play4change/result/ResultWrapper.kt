package com.ureka.play4change.result

import com.ureka.play4change.error.AppError
import com.ureka.play4change.result.throwable.ResultControlFlowException

// A singleton instance so it doesn't allocate memory every time happens a call to the block
val resultScopeInstance = object : ResultScope {}


inline fun <T> resultWrapper(block: ResultScope.() -> T): Result<T, AppError> {
    return try {
        Result.Success(resultScopeInstance.block())
    } catch (e: ResultControlFlowException) {
        Result.Error(e.error)
    }
}