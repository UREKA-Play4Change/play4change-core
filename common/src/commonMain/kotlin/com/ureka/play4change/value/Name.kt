package com.ureka.play4change.value

import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.result.Result
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Name private constructor(val value: String) {

    companion object {
        private val vClassName: String
            get() = this::class.simpleName.toString()

        fun create(raw: String?): Result<Name, AppError> {
            if(raw==null) return Result.Failure(
                BadRequest.InvalidField(vClassName,"invalid.nullable")
            )
            //TODO
            return Result.Success(Name(raw))
        }
    }

    override fun toString(): String = value
}