package com.ureka.play4change.result

import com.ureka.play4change.error.AppError

typealias AppResult<T> = Result<T, AppError>

sealed class Result<out T, out E> {
    data class Success<out T>(val data: T) : Result<T, Nothing>()
    data class Failure<out E>(val error: E) : Result<Nothing, E>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): E? = (this as? Failure)?.error

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (E) -> R,
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Failure -> onFailure(error)
    }

    inline fun onSuccess(block: (T) -> Unit): Result<T, E> {
        if (this is Success) block(data)
        return this
    }

    inline fun onFailure(block: (E) -> Unit): Result<T, E> {
        if (this is Failure) block(error)
        return this
    }

    inline fun <R> map(transform: (T) -> R): Result<R, E> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    inline fun <F> mapError(transform: (E) -> F): Result<T, F> = when (this) {
        is Success -> this
        is Failure -> Failure(transform(error))
    }
}