package com.ureka.play4change.core.component.error

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.component.base.ComponentEvents
import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.component.stateful.clearError
import com.ureka.play4change.core.error.UiError
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.error_auth_required
import play4change.composeapp.generated.resources.error_auth_title
import play4change.composeapp.generated.resources.error_network
import play4change.composeapp.generated.resources.error_network_title
import play4change.composeapp.generated.resources.error_not_found
import play4change.composeapp.generated.resources.error_not_found_title
import play4change.composeapp.generated.resources.error_service_title
import play4change.composeapp.generated.resources.error_service_unavailable
import play4change.composeapp.generated.resources.error_rate_limited
import play4change.composeapp.generated.resources.error_rate_limited_title
import play4change.composeapp.generated.resources.error_sign_in
import play4change.composeapp.generated.resources.error_try_again
import play4change.composeapp.generated.resources.error_unexpected
import play4change.composeapp.generated.resources.error_unexpected_title
import play4change.composeapp.generated.resources.ok

@Composable
fun <E : ComponentEvents, S : ComponentState> ErrorDialog(
    component: BaseComponent<S, E>,
    error: UiError?,
    onRetry: (() -> Unit)? = null
) {
    if (error == null) return

    val icon: ImageVector = when (error) {
        is UiError.ClientError.NetworkUnavailable -> Icons.Rounded.WifiOff
        is UiError.ClientError.Unauthorised       -> Icons.Rounded.Lock
        is UiError.ClientError.ValidationError    -> Icons.Rounded.Warning
        is UiError.ClientError.RateLimited        -> Icons.Rounded.Warning
        is UiError.ServerError.ServiceUnavailable -> Icons.Rounded.CloudOff
        is UiError.ServerError.NotFound           -> Icons.Rounded.Info
        is UiError.ServerError.Unexpected         -> Icons.Rounded.Warning
    }

    val iconTint = when (error) {
        is UiError.ServerError.NotFound -> MaterialTheme.colorScheme.secondary
        else                             -> MaterialTheme.colorScheme.error
    }

    val titleRes: StringResource = when (error) {
        is UiError.ClientError.NetworkUnavailable -> Res.string.error_network_title
        is UiError.ClientError.Unauthorised       -> Res.string.error_auth_title
        is UiError.ClientError.ValidationError    -> Res.string.error_unexpected_title
        is UiError.ClientError.RateLimited        -> Res.string.error_rate_limited_title
        is UiError.ServerError.ServiceUnavailable -> Res.string.error_service_title
        is UiError.ServerError.NotFound           -> Res.string.error_not_found_title
        is UiError.ServerError.Unexpected         -> Res.string.error_unexpected_title
    }

    val messageRes: StringResource = when (error) {
        is UiError.ClientError.NetworkUnavailable -> Res.string.error_network
        is UiError.ClientError.Unauthorised       -> Res.string.error_auth_required
        is UiError.ClientError.ValidationError    -> Res.string.error_unexpected
        is UiError.ClientError.RateLimited        -> Res.string.error_rate_limited
        is UiError.ServerError.ServiceUnavailable -> Res.string.error_service_unavailable
        is UiError.ServerError.NotFound           -> Res.string.error_not_found
        is UiError.ServerError.Unexpected         -> Res.string.error_unexpected
    }

    // Retry only makes sense for transient failures — auth and not-found require action,
    // not repetition.
    val retryable: Boolean = when (error) {
        is UiError.ClientError.NetworkUnavailable -> true
        is UiError.ClientError.Unauthorised       -> false
        is UiError.ClientError.ValidationError    -> false
        is UiError.ClientError.RateLimited        -> true
        is UiError.ServerError.ServiceUnavailable -> true
        is UiError.ServerError.NotFound           -> false
        is UiError.ServerError.Unexpected         -> true
    }

    val isAuthError = error is UiError.ClientError.Unauthorised
    val showRetry   = retryable && onRetry != null

    AlertDialog(
        onDismissRequest = { component.clearError() },
        icon = {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint
            )
        },
        title = {
            Text(
                text       = stringResource(titleRes),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text  = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        // Primary action on the right: "Try again" (filled) when retry is available,
        // otherwise "Sign in" or "OK" as a text button.
        confirmButton = {
            if (showRetry) {
                Button(onClick = { component.clearError(); onRetry!!() }) {
                    Text(stringResource(Res.string.error_try_again))
                }
            } else {
                TextButton(onClick = { component.clearError() }) {
                    Text(
                        stringResource(
                            if (isAuthError) Res.string.error_sign_in else Res.string.ok
                        )
                    )
                }
            }
        },
        // Secondary action on the left: "OK" to dismiss when retry is also shown.
        dismissButton = if (showRetry) {
            { TextButton(onClick = { component.clearError() }) { Text(stringResource(Res.string.ok)) } }
        } else null
    )
}
