package com.ureka.play4change.features.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.ureka.play4change.features.auth.presentation.LoginEvents
import com.ureka.play4change.features.auth.presentation.LoginLoadingAction
import com.ureka.play4change.features.auth.presentation.LoginState
import com.ureka.play4change.features.auth.presentation.isEmailLoading
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.login_cta
import play4change.composeapp.generated.resources.login_debug_token_helper
import play4change.composeapp.generated.resources.login_debug_token_label
import play4change.composeapp.generated.resources.login_debug_verify
import play4change.composeapp.generated.resources.login_email_error_invalid
import play4change.composeapp.generated.resources.login_email_label
import play4change.composeapp.generated.resources.login_link_expires
import play4change.composeapp.generated.resources.login_link_sent_body
import play4change.composeapp.generated.resources.login_link_sent_title
import play4change.composeapp.generated.resources.login_resend
import play4change.composeapp.generated.resources.login_resend_countdown
import play4change.composeapp.generated.resources.login_subtitle
import play4change.composeapp.generated.resources.login_welcome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(component: DefaultLoginComponent) {
    BaseView(
        component = component,
        topBar = {}
    ) { state, onEvent, innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // ── Decorative background blobs ───────────────────────────────
            Box(
                modifier = Modifier
                    .size(460.dp)
                    .offset(y = (-160).dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .align(Alignment.TopCenter)
            )
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .offset(x = (-100).dp, y = 100.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        CircleShape
                    )
                    .align(Alignment.BottomStart)
            )

            // ── Content ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.l),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(Spacing.xxxl))

                UrekaLogo(size = LogoSize.Large)

                Spacer(Modifier.height(Spacing.xxl))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                ) {
                    AnimatedContent(
                        targetState = state.linkSent,
                        transitionSpec = {
                            slideInVertically { it / 3 } + fadeIn(tween(320)) togetherWith
                            slideOutVertically { -it / 3 } + fadeOut(tween(200))
                        },
                        label = "login_stage",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.xl)
                    ) { linkSent ->
                        if (linkSent) {
                            LinkSentContent(
                                email = state.email,
                                countdown = state.resendCountdown,
                                isLoading = state.isEmailLoading,
                                onResend = { onEvent(LoginEvents.Resend) }
                            )
                        } else {
                            LoginFormContent(
                                state = state,
                                onEmailChange = { onEvent(LoginEvents.EmailChanged(it)) },
                                onSubmit = { onEvent(LoginEvents.Submit) }
                            )
                        }
                    }
                }

                // Token input — open below card once link is sent
                AnimatedVisibility(
                    visible = state.linkSent,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = Spacing.m)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s)
                    ) {
                        OutlinedTextField(
                            value = state.tokenInput,
                            onValueChange = { onEvent(LoginEvents.TokenChanged(it)) },
                            label = { Text(stringResource(Res.string.login_debug_token_label)) },
                            supportingText = {
                                Text(stringResource(Res.string.login_debug_token_helper))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        Button(
                            onClick = { onEvent(LoginEvents.VerifyToken) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.tokenInput.isNotBlank() &&
                                state.loadingAction !is LoginLoadingAction.Token,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (state.loadingAction is LoginLoadingAction.Token) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(Res.string.login_debug_verify))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xxxl))
            }
        }
    }
}

// ── Email form ────────────────────────────────────────────────────────────────

@Composable
private fun LoginFormContent(
    state: LoginState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(Res.string.login_welcome),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(Res.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xl))

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(Res.string.login_email_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            isError = state.emailError != null,
            supportingText = {
                state.emailError?.let {
                    Text(
                        text = stringResource(Res.string.login_email_error_invalid),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(Spacing.m))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !state.isEmailLoading &&
                state.email.isNotBlank() &&
                state.emailError == null,
            shape = MaterialTheme.shapes.medium
        ) {
            if (state.isEmailLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(Res.string.login_cta),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ── Link sent ─────────────────────────────────────────────────────────────────

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
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.s))

        val bodyText = buildAnnotatedString {
            append(stringResource(Res.string.login_link_sent_body, ""))
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            ) { append(email) }
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
            color = MaterialTheme.colorScheme.outlineVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Spacing.xl))

        TextButton(
            onClick = onResend,
            enabled = !isLoading && countdown == 0
        ) {
            Text(
                text = if (countdown > 0)
                    stringResource(Res.string.login_resend_countdown, countdown)
                else
                    stringResource(Res.string.login_resend)
            )
        }
    }
}

// ── Animated check ────────────────────────────────────────────────────────────

@Composable
private fun AnimatedCheckCircle() {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(700, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "check"
    )
    val color = MaterialTheme.colorScheme.secondary

    Canvas(Modifier.size(88.dp)) {
        val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color.copy(alpha = 0.12f))
        drawCircle(color, style = stroke)
        if (progress > 0f) {
            val startX = size.width * 0.25f; val startY = size.height * 0.50f
            val midX   = size.width * 0.45f; val midY   = size.height * 0.65f
            val endX   = size.width * 0.75f; val endY   = size.height * 0.35f
            val path = Path()
            if (progress < 0.5f) {
                val t = progress / 0.5f
                path.moveTo(startX, startY)
                path.lineTo(startX + (midX - startX) * t, startY + (midY - startY) * t)
            } else {
                val t = (progress - 0.5f) / 0.5f
                path.moveTo(startX, startY)
                path.lineTo(midX, midY)
                path.lineTo(midX + (endX - midX) * t, midY + (endY - midY) * t)
            }
            drawPath(path, color, style = stroke)
        }
    }
}
