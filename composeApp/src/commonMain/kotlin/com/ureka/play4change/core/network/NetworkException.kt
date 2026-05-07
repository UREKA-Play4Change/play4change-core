package com.ureka.play4change.core.network

/**
 * Thrown by HTTP repository implementations when the server returns a
 * non-success HTTP status that maps to a specific [NetworkError].
 *
 * Catch [NetworkException] at the use-case or ViewModel boundary and
 * convert [error] to UI state. Never propagate raw HTTP exceptions to the UI.
 */
class NetworkException(val error: NetworkError) : Exception(error.toString())
