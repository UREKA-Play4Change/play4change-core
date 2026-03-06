package com.ureka.play4change.core.component.error

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.component.base.ComponentEvents
import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.component.stateful.clearError
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res


@Composable
fun <E : ComponentEvents, S : ComponentState> ErrorDialog(
    component: BaseComponent<S, E>,
    error: Any?
) {
    if(component.state.value.error == null) return
    AlertDialog(
        onDismissRequest = { component.clearError() },
        title = {
//            Text(
//                text = error!!.code,
//                style = TextStyle(fontSize = TextSize.H3)
//            )
        },
        text = {
//            Text(
//                text = "${error?.incidentId}",
//                style = TextStyle(fontSize = TextSize.Caption)
//            )
        },
        confirmButton = {
//            TextButton(onClick = { component.clearError() }) {
//                Text(stringResource(Res.string.ok))
//            }
        }
    )
}