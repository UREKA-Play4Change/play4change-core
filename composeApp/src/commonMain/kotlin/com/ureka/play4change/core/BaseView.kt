package com.ureka.play4change.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.component.base.ComponentEvents
import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.component.error.ErrorDialog

/**
 * Standard wrapper for all screens.
 *
 * Provides a [Scaffold] with optional [ModalNavigationDrawer].
 *
 * All three active slots — [topBar], [bottomBar], and [content] — receive
 * the component's current [state] and [onEvent] dispatcher so every part
 * of the screen can react to state and fire events without passing lambdas
 * manually through intermediate composables.
 *
 * [content] also receives [innerPadding] from the Scaffold so scroll
 * containers can apply it as contentPadding.
 *
 * Usage — screen with a stateful bottom bar:
 *
 *   BaseView(
 *       component = component,
 *       bottomBar = { state, onEvent ->
 *           Button(
 *               onClick = { onEvent(MyEvents.Submit) },
 *               enabled = state.isValid
 *           ) { Text("Submit") }
 *       }
 *   ) { state, onEvent, innerPadding ->
 *       // main content
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <S : ComponentState, E : ComponentEvents> BaseView(
    component: BaseComponent<S, E>,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable (state: S, onEvent: (E) -> Unit) -> Unit = { _, _ -> },
    drawerState: DrawerState? = null,
    drawerContent: @Composable ColumnScope.() -> Unit = {},
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable (state: S, onEvent: (E) -> Unit, innerPadding: PaddingValues) -> Unit,
) {
    val state by component.state.subscribeAsState()

    val scaffold: @Composable () -> Unit = {
        Scaffold(
            topBar = topBar,
            bottomBar = { bottomBar(state, component::onEvent) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = contentAlignment
            ) {
                content(state, component::onEvent, innerPadding)
                ErrorDialog(component, state.error)
            }
        }
    }

    if (drawerState != null) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { ModalDrawerSheet(content = drawerContent) }
        ) {
            scaffold()
        }
    } else {
        scaffold()
    }
}
