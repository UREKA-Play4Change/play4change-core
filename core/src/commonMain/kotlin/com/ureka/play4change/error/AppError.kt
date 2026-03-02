package com.ureka.play4change.error

interface AppError {
    val httpStatus: Int
    val messageKey: String
    val params: List<Any>
}