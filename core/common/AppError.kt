package com.example.lottoinsight.core.common

sealed interface AppError {
    data object NetworkUnavailable : AppError
    data object ServerUnavailable : AppError
    data class HttpError(val code: Int, val message: String? = null) : AppError
    data class ParseError(val reason: String) : AppError
    data class InvalidDraw(val drawNo: Int, val reason: String) : AppError
    data object InsufficientData : AppError
    data class DatabaseError(val reason: String) : AppError
    data object Cancelled : AppError
}
