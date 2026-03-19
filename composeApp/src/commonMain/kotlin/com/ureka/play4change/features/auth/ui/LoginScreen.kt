package com.ureka.play4change.features.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.LogoSize
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
import play4change.composeapp.generated.resources.login_link_sent_body
import play4change.composeapp.generated.resources.login_link_sent_title
import play4change.composeapp.generated.resources.login_resend
import play4change.composeapp.generated.resources.login_resend_countdown
import play4change.composeapp.generated.resources.login_subtitle
import play4change.composeapp.generated.resources.login_welcome

@Composable
fun LoginScreen(
    component: DefaultLoginComponent,
    onNavigateToAbout: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    LaunchedEffect(component) {
        component.effects.collect { effect ->
            when (effect as LoginEffect) {
                LoginEffect.NavigateToAbout -> onNavigateToAbout()
                LoginEffect.NavigateToHome  -> onNavigateToHome()
            }
        }
    }

    BaseView(component = component) { state, onEvent ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.xxxl + Spacing.xl))

            UrekaLogo(size = LogoSize.Medium)

            Spacer(Modifier.height(Spacing.xxl))

            AnimatedContent(
                targetState = state.linkSent,
                transitionSpec = {
                    slideInVertically { it / 2 } + fadeIn() togetherWith
                    slideOutVertically { -it / 2 } + fadeOut()
                },
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(Res.string.login_welcome),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(Res.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xxl))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(Res.string.login_email_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            isError = emailError != null,
            supportingText = {
                emailError?.let {
                    Text(stringResource(Res.string.login_email_error_invalid),
                        color = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(Spacing.l))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !isLoading && email.isNotBlank() && emailError == null,
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(Res.string.login_cta), style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(Spacing.l))

        TextButton(onClick = onAbout) {
            Text(stringResource(Res.string.login_about_link), color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(Spacing.xl))
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedCheckCircle()

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text = stringResource(Res.string.login_link_sent_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(Spacing.m))

        // Email bolded in body text
        val bodyText = buildAnnotatedString {
            append(stringResource(Res.string.login_link_sent_body, ""))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)) {
                append(email)
            }
        }
        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(Res.string.login_link_expires),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xxl))

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
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun AnimatedCheckCircle() {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(600, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "check"
    )
    val checkColor = MaterialTheme.colorScheme.secondary

    Canvas(Modifier.size(80.dp)) {
        val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(checkColor.copy(alpha = 0.15f))
        drawCircle(checkColor, style = stroke)
        if (progress > 0f) {
            val startX = size.width * 0.25f; val startY = size.height * 0.5f
            val midX   = size.width * 0.45f; val midY   = size.height * 0.65f
            val endX   = size.width * 0.75f; val endY   = size.height * 0.35f
            val path = Path()
            if (progress < 0.5f) {
                val t = progress / 0.5f
                path.moveTo(startX, startY)
                path.lineTo(startX + (midX - startX) * t, startY + (midY - startY) * t)
            } else {
                val t = (progress - 0.5f) / 0.5f
                path.moveTo(startX, startY); path.lineTo(midX, midY)
                path.lineTo(midX + (endX - midX) * t, midY + (endY - midY) * t)
            }
            drawPath(path, checkColor, style = stroke)
        }
    }
}
