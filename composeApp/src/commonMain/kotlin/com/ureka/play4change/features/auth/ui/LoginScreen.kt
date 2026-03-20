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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.LogoSize
import com.ureka.play4change.design.components.UrekaLogo
import com.ureka.play4change.features.auth.domain.model.SocialProvider
import com.ureka.play4change.features.auth.presentation.AuthMode
import com.ureka.play4change.features.auth.presentation.DefaultLoginComponent
import com.ureka.play4change.features.auth.presentation.LoginEvents
import com.ureka.play4change.features.auth.presentation.LoginState
import com.ureka.play4change.features.auth.presentation.isEmailLoading
import com.ureka.play4change.features.auth.presentation.loadingProvider
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.auth_continue_facebook
import play4change.composeapp.generated.resources.auth_continue_google
import play4change.composeapp.generated.resources.auth_have_account
import play4change.composeapp.generated.resources.auth_name_label
import play4change.composeapp.generated.resources.auth_no_account
import play4change.composeapp.generated.resources.auth_or_divider
import play4change.composeapp.generated.resources.auth_register
import play4change.composeapp.generated.resources.auth_register_cta
import play4change.composeapp.generated.resources.auth_sign_in
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(component: DefaultLoginComponent) {
    BaseView(
        component = component,
        topBar = {
            CenterAlignedTopAppBar(
                title = { UrekaLogo(size = LogoSize.Small) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
    ) { state, onEvent, _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(Spacing.xxl))

            // ── Form card — elevated surface, slightly darker than background ──
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.l),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = state.linkSent,
                        transitionSpec = {
                            slideInVertically { it / 2 } + fadeIn() togetherWith
                            slideOutVertically { -it / 2 } + fadeOut()
                        },
                        label = "login_stage"
                    ) { linkSent ->
                        if (linkSent) {
                            LinkSentContent(
                                email = state.email,
                                countdown = state.resendCountdown,
                                isLoading = state.isEmailLoading,
                                onResend = { onEvent(LoginEvents.Resend) }
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                AnimatedContent(
                                    targetState = state.mode,
                                    transitionSpec = {
                                        slideInVertically { it / 3 } + fadeIn(tween(250)) togetherWith
                                        slideOutVertically { -it / 3 } + fadeOut(tween(200))
                                    },
                                    label = "auth_mode"
                                ) { mode ->
                                    if (mode == AuthMode.Login) {
                                        LoginFormContent(
                                            state = state,
                                            onEmailChange = { onEvent(LoginEvents.EmailChanged(it)) },
                                            onSubmit = { onEvent(LoginEvents.Submit) }
                                        )
                                    } else {
                                        RegisterFormContent(
                                            state = state,
                                            onNameChange = { onEvent(LoginEvents.NameChanged(it)) },
                                            onEmailChange = { onEvent(LoginEvents.EmailChanged(it)) },
                                            onSubmit = { onEvent(LoginEvents.Submit) }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(Spacing.l))
                                OrDivider()
                                Spacer(Modifier.height(Spacing.m))
                                val anyLoading = state.loadingAction != null
                                GoogleSignInButton(
                                    onClick = { onEvent(LoginEvents.SocialLogin(SocialProvider.GOOGLE)) },
                                    isLoading = state.loadingProvider == SocialProvider.GOOGLE,
                                    enabled = !anyLoading || state.loadingProvider == SocialProvider.GOOGLE
                                )
                                Spacer(Modifier.height(Spacing.xs))
                                FacebookSignInButton(
                                    onClick = { onEvent(LoginEvents.SocialLogin(SocialProvider.FACEBOOK)) },
                                    isLoading = state.loadingProvider == SocialProvider.FACEBOOK,
                                    enabled = !anyLoading || state.loadingProvider == SocialProvider.FACEBOOK
                                )
                                Spacer(Modifier.height(Spacing.xl))
                                ToggleModeRow(
                                    mode = state.mode,
                                    onToggle = { onEvent(LoginEvents.ToggleMode) }
                                )
                                Spacer(Modifier.height(Spacing.m))
                            }
                        }
                    }
                }
            }
            // ── End of card ────────────────────────────────────────────────

            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun LoginFormContent(
    state: LoginState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit
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
            enabled = !state.isEmailLoading && state.email.isNotBlank() && state.emailError == null,
            shape = MaterialTheme.shapes.medium
        ) {
            if (state.isEmailLoading) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(Res.string.login_cta), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun RegisterFormContent(
    state: LoginState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(Res.string.auth_register),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(Spacing.xl))

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(Res.string.auth_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.nameError != null,
            supportingText = { state.nameError?.let { Text(it) } },
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(Spacing.xs))

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
                    Text(stringResource(Res.string.login_email_error_invalid),
                        color = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(Spacing.m))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !state.isEmailLoading && state.name.isNotBlank() && state.email.isNotBlank(),
            shape = MaterialTheme.shapes.medium
        ) {
            if (state.isEmailLoading) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(Res.string.auth_register_cta))
            }
        }
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            stringResource(Res.string.auth_or_divider),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.s)
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit, isLoading: Boolean, enabled: Boolean) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F)
        ),
        border = BorderStroke(1.dp, Color(0xFFDADADA)),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            GoogleGIcon(Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.s))
            Text(
                stringResource(Res.string.auth_continue_google),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun GoogleGIcon(modifier: Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2; val cy = size.height / 2; val r = size.width * 0.45f
        val colors = listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05), Color(0xFFEA4335))
        val sweeps = listOf(90f, 90f, 90f, 90f)
        var startAngle = -30f
        colors.forEachIndexed { i, c ->
            drawArc(
                c, startAngle, sweeps[i], false,
                topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2),
                style = Stroke(width = size.width * 0.15f, cap = StrokeCap.Butt)
            )
            startAngle += sweeps[i]
        }
    }
}

@Composable
private fun FacebookSignInButton(onClick: () -> Unit, isLoading: Boolean, enabled: Boolean) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1877F2),
            contentColor = Color.White
        ),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text("f", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Spacer(Modifier.width(Spacing.s))
            Text(
                stringResource(Res.string.auth_continue_facebook),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ToggleModeRow(mode: AuthMode, onToggle: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            stringResource(
                if (mode == AuthMode.Login) Res.string.auth_no_account
                else Res.string.auth_have_account
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = onToggle,
            contentPadding = PaddingValues(start = 4.dp)
        ) {
            Text(
                stringResource(
                    if (mode == AuthMode.Login) Res.string.auth_register
                    else Res.string.auth_sign_in
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
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
