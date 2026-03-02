package com.ureka.play4change.result.extensions

import com.ureka.play4change.result.Result

inline fun <T, E, R> Result<T, E>.fold(
    onSuccess: (T) -> R,
    onError: (E) -> R
): R {
    return when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onError(error)
    }
}