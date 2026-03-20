package com.ureka.play4change.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ureka.play4change.design.Spacing

enum class DayStatus { Completed, Today, Future }

@Composable
fun DayDots(
    weekProgress: List<DayStatus>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        weekProgress.forEach { status ->
            val color = when (status) {
                DayStatus.Completed -> MaterialTheme.colorScheme.secondary
                DayStatus.Today     -> MaterialTheme.colorScheme.primary
                DayStatus.Future    -> MaterialTheme.colorScheme.surfaceVariant
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
        }
    }
}
