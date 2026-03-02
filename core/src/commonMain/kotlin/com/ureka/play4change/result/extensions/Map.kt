package com.ureka.play4change.result.extensions

import com.ureka.play4change.result.Result

inline fun <T, E, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> {
    return when(this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
    }
}