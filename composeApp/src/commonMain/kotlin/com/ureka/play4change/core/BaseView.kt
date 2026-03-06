package com.ureka.play4change.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.component.base.ComponentEvents
import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.component.error.ErrorDialog

/**
 * A standard wrapper for all screens.
 * Automatically subscribes to the component's state and passes it to the content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <S : ComponentState, E : ComponentEvents> BaseView(
    component: BaseComponent<S, E>,
    screenAlignment: Alignment = Alignment.Center,
    content: @Composable (state: S, onEvent: (E) -> Unit) -> Unit,
) {
    val state by component.state.subscribeAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = screenAlignment
    ) {
        content(state, component::onEvent)
        ErrorDialog(component, state.error)
    }
}
