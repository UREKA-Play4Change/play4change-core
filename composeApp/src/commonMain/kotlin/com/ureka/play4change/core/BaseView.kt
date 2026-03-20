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
 * Provides a [Scaffold] with optional [ModalNavigationDrawer].
 * The [content] lambda receives the current [state], the [onEvent] dispatcher,
 * and [innerPadding] from the Scaffold so each screen can apply insets correctly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <S : ComponentState, E : ComponentEvents> BaseView(
    component: BaseComponent<S, E>,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    drawerState: DrawerState? = null,
    drawerContent: @Composable ColumnScope.() -> Unit = {},
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable (state: S, onEvent: (E) -> Unit, innerPadding: PaddingValues) -> Unit,
) {
    val state by component.state.subscribeAsState()

    if (drawerState != null) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { ModalDrawerSheet(content = drawerContent) }
        ) {
            Scaffold(
                topBar = topBar,
                bottomBar = bottomBar,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(Modifier.fillMaxSize(), contentAlignment = contentAlignment) {
                    content(state, component::onEvent, innerPadding)
                    ErrorDialog(component, state.error)
                }
            }
        }
    } else {
        Scaffold(
            topBar = topBar,
            bottomBar = bottomBar,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(Modifier.fillMaxSize(), contentAlignment = contentAlignment) {
                content(state, component::onEvent, innerPadding)
                ErrorDialog(component, state.error)
            }
        }
    }
}
