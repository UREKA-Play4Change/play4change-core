package com.ureka.play4change.value

import com.ureka.play4change.error.AppError
import com.ureka.play4change.result.Result
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Password private constructor(val value: String) {

    companion object {
        private const val MIN_LENGTH = 8

        fun create(raw: String?): Result<Password, AppError> {
            if(raw==null) return Result.Failure(AppError)
            //TODO
            return Result.Success(Password(raw))
        }
    }

    override fun toString(): String = "*".repeat(MIN_LENGTH)
}