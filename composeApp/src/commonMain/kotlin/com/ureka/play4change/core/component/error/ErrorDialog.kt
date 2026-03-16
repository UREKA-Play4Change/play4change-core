package com.ureka.play4change.core.component.error

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.component.base.ComponentEvents
import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.component.stateful.clearError
import com.ureka.play4change.core.error.AppError
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.error_network
import play4change.composeapp.generated.resources.error_unexpected
import play4change.composeapp.generated.resources.ok

@Composable
fun <E : ComponentEvents, S : ComponentState> ErrorDialog(
    component: BaseComponent<S, E>,
    error: AppError?
) {
    if (error == null) return
    val message = when (error) {
        is AppError.ClientError.NetworkUnavailable -> stringResource(Res.string.error_network)
        is AppError.ClientError.ValidationError -> stringResource(Res.string.error_unexpected)
        is AppError.ClientError.Unauthorised -> stringResource(Res.string.error_unexpected)
        is AppError.ServerError.ServiceUnavailable -> stringResource(Res.string.error_unexpected)
        is AppError.ServerError.NotFound -> stringResource(Res.string.error_unexpected)
        is AppError.ServerError.Unexpected -> stringResource(Res.string.error_unexpected)
    }
    AlertDialog(
        onDismissRequest = { component.clearError() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = error.messageKey,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = { component.clearError() }) {
                Text(stringResource(Res.string.ok))
            }
        }
    )
}
