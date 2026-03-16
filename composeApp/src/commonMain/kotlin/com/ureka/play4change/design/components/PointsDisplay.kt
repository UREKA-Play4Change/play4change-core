package com.ureka.play4change.design.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ureka.play4change.design.MotionTokens
import com.ureka.play4change.design.Spacing
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.home_pts_suffix

@Composable
fun PointsDisplay(
    points: Int,
    modifier: Modifier = Modifier
) {
    val animatedPoints by animateIntAsState(
        targetValue = points,
        animationSpec = tween(durationMillis = MotionTokens.DurationLong2),
        label = "points"
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Baseline
    ) {
        Text(
            text = "⭐",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = "$animatedPoints",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = stringResource(Res.string.home_pts_suffix),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
