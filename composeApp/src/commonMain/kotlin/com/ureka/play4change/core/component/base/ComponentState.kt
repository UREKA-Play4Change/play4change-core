package com.ureka.play4change.core.component.base

import com.ureka.play4change.core.error.AppError

interface ComponentState {
    val isLoading: Boolean
    val error: AppError?
}
