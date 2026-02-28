package com.ureka.play4change.core.component.base

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.ureka.play4change.core.component.stateful.StatefulComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class BaseComponent<S: ComponentState, E: ComponentEvents>(
    componentContext: ComponentContext,
    initialState: S
): ComponentContext by componentContext, StatefulComponent<S> {
    private val _state = MutableValue(initialState)
    override val state: Value<S> = _state

    protected val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())


    init {
        lifecycle.doOnDestroy {
            scope.cancel()
        }
    }

    abstract fun onEvent(event: E)

    override fun updateState(reducer: (S) -> S) {
        _state.update(reducer)
    }
}