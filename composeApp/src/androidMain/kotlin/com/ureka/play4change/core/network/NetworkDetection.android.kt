package com.ureka.play4change.core.network

import java.io.IOException

internal actual fun isNetworkUnavailableError(e: Throwable): Boolean =
    e is IOException || e.cause is IOException
