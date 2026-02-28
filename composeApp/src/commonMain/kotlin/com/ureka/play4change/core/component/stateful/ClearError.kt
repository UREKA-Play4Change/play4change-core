package com.ureka.play4change.core.component.stateful

import com.ureka.play4change.core.component.base.ComponentState

fun <S : ComponentState> StatefulComponent<S>.clearError() {
    updateState { copyBase(error = null) }
}