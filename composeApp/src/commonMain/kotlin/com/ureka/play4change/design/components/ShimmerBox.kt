package com.ureka.play4change.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ureka.play4change.design.Spacing

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Box(
        modifier = modifier
            .alpha(alpha)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
    )
}

@Composable
fun HeroCardShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
    ) {
        ShimmerBox(Modifier.fillMaxWidth().height(24.dp))
        Spacer(Modifier.height(Spacing.xs))
        ShimmerBox(Modifier.width(180.dp).height(40.dp))
        Spacer(Modifier.height(Spacing.xs))
        ShimmerBox(Modifier.fillMaxWidth().height(12.dp))
        Spacer(Modifier.height(Spacing.m))
    }
}

@Composable
fun TaskCardShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .padding(Spacing.l)
    ) {
        ShimmerBox(Modifier.width(120.dp).height(24.dp))
        Spacer(Modifier.height(Spacing.xs))
        ShimmerBox(Modifier.fillMaxWidth().height(20.dp))
        Spacer(Modifier.height(4.dp))
        ShimmerBox(Modifier.width(200.dp).height(20.dp))
        Spacer(Modifier.height(Spacing.m))
        ShimmerBox(Modifier.fillMaxWidth().height(48.dp).clip(MaterialTheme.shapes.medium))
    }
}

@Composable
fun RoadmapNodeShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShimmerBox(Modifier.size(56.dp).clip(CircleShape))
        Spacer(Modifier.height(4.dp))
        ShimmerBox(Modifier.width(64.dp).height(12.dp))
    }
}
