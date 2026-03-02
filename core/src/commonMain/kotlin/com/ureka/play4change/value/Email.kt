package com.ureka.play4change.value

import com.ureka.play4change.error.AppError
import com.ureka.play4change.result.Result
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Email private constructor(val value: String) {

    init {
        require(isValid(value)) { AppError }
    }

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()

        fun create(raw: String?): Result<Email, AppError> {
            if (raw==null) return Result.Failure(AppError)
            //TODO
            return Result.Success(Email(trimmed.lowercase()))
        }

        private fun isValid(s: String): Boolean = EMAIL_REGEX.matches(s)
    }

    override fun toString(): String = value
}