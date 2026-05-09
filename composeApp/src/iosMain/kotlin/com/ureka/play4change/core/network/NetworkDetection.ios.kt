package com.ureka.play4change.core.network

internal actual fun isNetworkUnavailableError(e: Throwable): Boolean {
    val msg = e.message ?: return false
    return msg.contains("Could not connect to the server", ignoreCase = true) ||
        msg.contains("The Internet connection appears to be offline", ignoreCase = true) ||
        msg.contains("Connection refused", ignoreCase = true) ||
        msg.contains("could not connect", ignoreCase = true) ||
        msg.contains("network connection was lost", ignoreCase = true) ||
        msg.contains("A server with the specified hostname could not be found", ignoreCase = true)
}
