package com.ureka.play4change.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class SessionEvent {
    data object SessionExpired : SessionEvent()
}

/**
 * Application-wide bus for session lifecycle events.
 * The root component observes [events] and navigates to the login screen
 * when [SessionExpired] is received.
 */
object SessionEventBus {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    fun sessionExpired() {
        _events.tryEmit(SessionEvent.SessionExpired)
    }
}
