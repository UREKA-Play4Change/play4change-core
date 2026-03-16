package com.ureka.play4change.features.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.UrekaLogo
import com.ureka.play4change.features.auth.presentation.DefaultLoginComponent
import com.ureka.play4change.features.auth.presentation.LoginEffect
import com.ureka.play4change.features.auth.presentation.LoginEvents
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.login_about_link
import play4change.composeapp.generated.resources.login_cta
import play4change.composeapp.generated.resources.login_email_error_invalid
import play4change.composeapp.generated.resources.login_email_label
import play4change.composeapp.generated.resources.login_link_expires
import play4change.composeapp.generated.resources.login_link_sent_title
import play4change.composeapp.generated.resources.login_resend
import play4change.composeapp.generated.resources.login_resend_countdown
import play4change.composeapp.generated.resources.login_subtitle
import play4change.composeapp.generated.resources.login_welcome

@Composable
fun LoginScreen(
    component: DefaultLoginComponent,
    onNavigateToAbout: () -> Unit
) {
    LaunchedEffect(component) {
        component.effects.collect { effect ->
            when (effect as LoginEffect) {
                LoginEffect.NavigateToAbout -> onNavigateToAbout()
                LoginEffect.NavigateToHome  -> { /* handled by root */ }
            }
        }
    }

    BaseView(component = component) { state, onEvent ->
        AnimatedContent(
            targetState = state.linkSent,
            label = "login_content"
        ) { linkSent ->
            if (linkSent) {
                LinkSentContent(
                    email = state.email,
                    countdown = state.resendCountdown,
                    isLoading = state.isLoading,
                    onResend = { onEvent(LoginEvents.Resend) }
                )
            } else {
                EmailInputContent(
                    email = state.email,
                    emailError = state.emailError,
                    isLoading = state.isLoading,
                    onEmailChange = { onEvent(LoginEvents.EmailChanged(it)) },
                    onSubmit = { onEvent(LoginEvents.Submit) },
                    onAbout = { onEvent(LoginEvents.OpenAbout) }
                )
            }
        }
    }
}

@Composable
private fun EmailInputContent(
    email: String,
    emailError: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onAbout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        UrekaLogo()
        Spacer(modifier = Modifier.height(Spacing.xxxl))
        Text(
            text = stringResource(Res.string.login_welcome),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.s))
        Text(
            text = stringResource(Res.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xxl))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(Res.string.login_email_label)) },
            isError = emailError != null,
            supportingText = if (emailError != null) {
                { Text(stringResource(Res.string.login_email_error_invalid)) }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(Spacing.l))
        Button(
            onClick = onSubmit,
            enabled = !isLoading && email.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(Res.string.login_cta))
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xl))
        TextButton(onClick = onAbout) {
            Text(
                text = stringResource(Res.string.login_about_link),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun LinkSentContent(
    email: String,
    countdown: Int,
    isLoading: Boolean,
    onResend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📬",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(Spacing.xl))
        Text(
            text = stringResource(Res.string.login_link_sent_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.m))
        Text(
            text = stringResource(Res.string.login_link_expires),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xxl))
        TextButton(
            onClick = onResend,
            enabled = !isLoading && countdown == 0
        ) {
            if (countdown > 0) {
                Text(stringResource(Res.string.login_resend_countdown, countdown))
            } else {
                Text(stringResource(Res.string.login_resend))
            }
        }
    }
}
