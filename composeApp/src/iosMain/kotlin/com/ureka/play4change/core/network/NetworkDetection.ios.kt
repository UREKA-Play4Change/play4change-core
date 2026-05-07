package com.ureka.play4change.core.network

internal actual fun isNetworkUnavailableError(e: Throwable): Boolean =
    e.message?.contains("Could not connect to the server") == true ||
        e.message?.contains("The Internet connection appears to be offline") == true
