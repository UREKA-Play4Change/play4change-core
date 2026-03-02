package com.ureka.play4change.result

import com.ureka.play4change.result.throwable.ResultControlFlowException

/**
 * Singleton scope – no allocations per call.
 * All domain functions should return [AppResult] by delegating to [resultWrapper].
 */
val resultScopeInstance: ResultScope = object : ResultScope {}

/**
 * Runs [block] inside a [ResultScope].
 * Any call to [ResultScope.bind] that encounters a [Result.Failure], or any call to
 * [ResultScope.ensure] / [ResultScope.ensureNotNull] that fails, will short-circuit and
 * be captured as [Result.Failure].
 *
 * All other exceptions propagate normally (they are NOT suppressed).
 */
inline fun <T> resultWrapper(block: ResultScope.() -> T): AppResult<T> {
    return try {
        Result.Success(resultScopeInstance.block())
    } catch (e: ResultControlFlowException) {
        Result.Failure(e.error)
    }
}

/**
 * Suspend-aware variant for coroutine contexts (Ktor, repository layer, etc.).
 */
suspend inline fun <T> suspendResultWrapper(
    crossinline block: suspend ResultScope.() -> T
): AppResult<T> {
    return try {
        Result.Success(resultScopeInstance.block())
    } catch (e: ResultControlFlowException) {
        Result.Failure(e.error)
    }
}
