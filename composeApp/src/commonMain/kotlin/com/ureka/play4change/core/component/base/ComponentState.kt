package com.ureka.play4change.core.component.base

import com.ureka.play4change.core.error.UiError

interface ComponentState {
    val isLoading: Boolean
    val error: UiError?
}
