package com.ureka.play4change.design.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ureka.play4change.design.Spacing

enum class BadgeSize { Compact, Featured }

@Composable
fun StreakBadge(
    streakDays: Int,
    size: BadgeSize = BadgeSize.Compact,
    modifier: Modifier = Modifier
) {
    val flameSize: Dp = if (size == BadgeSize.Compact) 18.dp else 28.dp
    val textStyle = if (size == BadgeSize.Compact) MaterialTheme.typography.labelMedium
                    else MaterialTheme.typography.titleMedium

    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor  = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "flame_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "flame_scale"
    )

    // Pre-compute pixel size outside DrawScope to avoid resolution ambiguity
    val flamePx = with(LocalDensity.current) { flameSize.toPx() }

    Row(
        modifier = modifier.padding(horizontal = Spacing.s, vertical = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .requiredSize(flameSize)
                .then(if (streakDays > 0) Modifier.scale(pulseScale) else Modifier)
        ) {
            val path = Path().apply {
                moveTo(flamePx * 0.5f, flamePx * 0.0f)
                cubicTo(
                    flamePx * 0.9f, flamePx * 0.3f,
                    flamePx * 0.8f, flamePx * 0.6f,
                    flamePx * 0.5f, flamePx * 1.0f
                )
                cubicTo(
                    flamePx * 0.2f, flamePx * 0.6f,
                    flamePx * 0.1f, flamePx * 0.3f,
                    flamePx * 0.5f, flamePx * 0.0f
                )
                close()
            }
            drawPath(path, color = if (streakDays > 0) tertiaryColor else outlineColor)
        }

        Spacer(Modifier.width(Spacing.xs))

        Text(
            text = "$streakDays",
            style = textStyle,
            color = if (streakDays > 0) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.outline
        )
    }
}
