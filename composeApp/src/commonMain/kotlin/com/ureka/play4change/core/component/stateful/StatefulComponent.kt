package com.ureka.play4change.core.component.stateful

import com.arkivanov.decompose.value.Value
import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError

interface StatefulComponent<S : ComponentState> {
    val state: Value<S>
    fun updateState(reducer: S.() -> S)
    fun S.copyBase(isLoading: Boolean = this.isLoading, error: AppError? = this.error): S
}
