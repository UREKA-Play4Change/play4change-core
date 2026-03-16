package com.ureka.play4change.design.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ureka.play4change.design.MotionTokens
import com.ureka.play4change.design.Spacing

enum class OptionState { Idle, Selected, Correct, Wrong }

@Composable
fun TaskOptionButton(
    text: String,
    optionState: OptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderWidth by animateDpAsState(
        targetValue = if (optionState != OptionState.Idle) 2.dp else 1.dp,
        animationSpec = spring(stiffness = MotionTokens.SpringStiffnessMedium),
        label = "border"
    )

    val borderColor = when (optionState) {
        OptionState.Idle     -> MaterialTheme.colorScheme.outline
        OptionState.Selected -> MaterialTheme.colorScheme.primary
        OptionState.Correct  -> MaterialTheme.colorScheme.secondary
        OptionState.Wrong    -> MaterialTheme.colorScheme.error
    }

    val containerColor = when (optionState) {
        OptionState.Idle     -> MaterialTheme.colorScheme.surface
        OptionState.Selected -> MaterialTheme.colorScheme.primaryContainer
        OptionState.Correct  -> MaterialTheme.colorScheme.secondaryContainer
        OptionState.Wrong    -> MaterialTheme.colorScheme.errorContainer
    }

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(borderWidth, borderColor),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
            containerColor = containerColor
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(Spacing.l)
        )
    }
}
