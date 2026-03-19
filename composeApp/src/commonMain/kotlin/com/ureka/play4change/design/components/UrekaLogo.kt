package com.ureka.play4change.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.ureka.play4change.design.Spacing
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.app_university

enum class LogoSize { Small, Medium, Large }

@Composable
fun UrekaLogo(
    size: LogoSize = LogoSize.Medium,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val wordmark = buildAnnotatedString {
        withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.ExtraBold)) { append("U") }
        withStyle(SpanStyle(color = secondaryColor, fontWeight = FontWeight.ExtraBold)) { append("!") }
        withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.ExtraBold)) { append("REKA") }
    }

    val wordmarkStyle = when (size) {
        LogoSize.Small  -> MaterialTheme.typography.headlineMedium
        LogoSize.Medium -> MaterialTheme.typography.displaySmall
        LogoSize.Large  -> MaterialTheme.typography.displayMedium
    }

    val animate = size != LogoSize.Small

    var visible by remember { mutableStateOf(!animate) }
    LaunchedEffect(Unit) { if (animate) visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = if (animate) {
            scaleIn(
                initialScale = 0.7f,
                animationSpec = tween(600, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
            ) + fadeIn(tween(600))
        } else {
            fadeIn(tween(0))
        }
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = wordmark, style = wordmarkStyle)

            Spacer(modifier = Modifier.height(Spacing.xxs))

            Text(
                text = stringResource(Res.string.app_university),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                letterSpacing = 0.15.em
            )
        }
    }
}
