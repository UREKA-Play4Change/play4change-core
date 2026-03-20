package com.ureka.play4change.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ureka.play4change.design.Spacing

enum class OptionState { Idle, Selected, Correct, Wrong }

@Composable
fun TaskOptionButton(
    text: String,
    optionState: OptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        when (optionState) {
            OptionState.Idle     -> MaterialTheme.colorScheme.surface
            OptionState.Selected -> MaterialTheme.colorScheme.primaryContainer
            OptionState.Correct  -> MaterialTheme.colorScheme.secondaryContainer
            OptionState.Wrong    -> MaterialTheme.colorScheme.errorContainer
        }, tween(200), label = "bg"
    )
    val borderColor by animateColorAsState(
        when (optionState) {
            OptionState.Idle     -> MaterialTheme.colorScheme.outline
            OptionState.Selected -> MaterialTheme.colorScheme.primary
            OptionState.Correct  -> MaterialTheme.colorScheme.secondary
            OptionState.Wrong    -> MaterialTheme.colorScheme.error
        }, tween(200), label = "border"
    )
    val textColor by animateColorAsState(
        when (optionState) {
            OptionState.Idle     -> MaterialTheme.colorScheme.onSurface
            OptionState.Selected -> MaterialTheme.colorScheme.onPrimaryContainer
            OptionState.Correct  -> MaterialTheme.colorScheme.onSecondaryContainer
            OptionState.Wrong    -> MaterialTheme.colorScheme.onErrorContainer
        }, tween(200), label = "text"
    )

    val scale by animateFloatAsState(
        if (optionState == OptionState.Selected) 1.02f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(optionState) {
        if (optionState == OptionState.Wrong) {
            for (offset in listOf(-10f, 10f, -8f, 8f, -5f, 5f, 0f)) {
                shakeOffset.animateTo(offset, tween(40))
            }
        }
    }

    val borderWidth by animateDpAsState(
        when (optionState) {
            OptionState.Correct  -> 3.dp
            OptionState.Idle     -> 1.dp
            else                 -> 2.dp
        },
        tween(200),
        label = "border_width"
    )

    Surface(
        onClick = onClick,
        enabled = optionState == OptionState.Idle || optionState == OptionState.Selected,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .offset { IntOffset(shakeOffset.value.toInt(), 0) },
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (optionState) {
                OptionState.Correct -> Icon(
                    Icons.Rounded.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                OptionState.Wrong -> Icon(
                    Icons.Rounded.Cancel, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                else -> Box(Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Spacing.s))
            Text(text, style = MaterialTheme.typography.bodyLarge, color = textColor)
        }
    }
}
