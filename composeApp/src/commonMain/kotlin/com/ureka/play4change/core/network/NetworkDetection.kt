package com.ureka.play4change.core.network

/**
 * Returns true if [e] represents a network-unavailable condition on the current platform.
 * Implemented via expect/actual: Android checks for [java.io.IOException];
 * iOS checks for [NSURLError] from Foundation networking.
 */
internal expect fun isNetworkUnavailableError(e: Throwable): Boolean
