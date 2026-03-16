package com.ureka.play4change.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ureka.play4change.core.model.NodeStatus
import com.ureka.play4change.core.model.RoadmapNode
import com.ureka.play4change.design.MotionTokens
import com.ureka.play4change.design.Spacing

@Composable
fun RoadmapView(
    nodes: List<RoadmapNode>,
    modifier: Modifier = Modifier,
    onNodeClick: (RoadmapNode) -> Unit
) {
    Column(modifier = modifier) {
        nodes.forEachIndexed { index, node ->
            RoadmapNodeItem(
                node = node,
                isLast = index == nodes.lastIndex,
                onNodeClick = onNodeClick
            )
        }
    }
}

@Composable
private fun RoadmapNodeItem(
    node: RoadmapNode,
    isLast: Boolean,
    onNodeClick: (RoadmapNode) -> Unit
) {
    val primaryColor      = MaterialTheme.colorScheme.primary
    val secondaryColor    = MaterialTheme.colorScheme.secondary
    val tertiaryColor     = MaterialTheme.colorScheme.tertiary
    val surfaceVariant    = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainer  = MaterialTheme.colorScheme.primaryContainer
    val outlineColor      = MaterialTheme.colorScheme.outline

    val horizontalOffset = if (node.isAdaptiveBranch) 60.dp else 0.dp

    val pulseTransition = rememberInfiniteTransition(label = "pulse_${node.dayIndex}")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MotionTokens.PulseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MotionTokens.PulseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .offset(x = horizontalOffset)
            .padding(vertical = Spacing.s)
    ) {
        // Connector line drawn below (except last node)
        if (!isLast) {
            val lineColor = if (node.isAdaptiveBranch) tertiaryColor else outlineColor
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 26.dp)
                    .size(width = 4.dp, height = 40.dp)
            ) {
                if (node.isAdaptiveBranch) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        cubicTo(
                            size.width / 2f, size.height * 0.3f,
                            size.width / 2f, size.height * 0.7f,
                            size.width / 2f, size.height
                        )
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 4f)
                    )
                } else {
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 4f
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Animated pulse ring for Current node
                if (node.status == NodeStatus.Current) {
                    Canvas(modifier = Modifier.size((52 * pulseScale).dp)) {
                        drawCircle(
                            color = primaryColor.copy(alpha = pulseAlpha * 0.4f),
                            radius = size.minDimension / 2f
                        )
                    }
                }

                // Node circle
                val (fillColor, strokeColor) = when (node.status) {
                    NodeStatus.Completed -> secondaryColor to secondaryColor
                    NodeStatus.Current   -> primaryColor to primaryColor
                    NodeStatus.Available -> primaryContainer to primaryColor
                    NodeStatus.Locked    -> surfaceVariant to outlineColor
                }

                val isClickable = node.status == NodeStatus.Completed ||
                        node.status == NodeStatus.Current

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(fillColor)
                        .then(
                            if (isClickable)
                                Modifier.clickable { onNodeClick(node) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (node.status) {
                        NodeStatus.Completed -> "✓"
                        NodeStatus.Current   -> "▶"
                        NodeStatus.Available -> "○"
                        NodeStatus.Locked    -> "🔒"
                    }
                    Text(
                        text = icon,
                        color = when (node.status) {
                            NodeStatus.Completed, NodeStatus.Current -> Color.White
                            NodeStatus.Available -> primaryColor
                            NodeStatus.Locked    -> outlineColor
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Points badge (top-right)
                if (node.pointsReward > 0 &&
                    (node.status == NodeStatus.Completed || node.status == NodeStatus.Available)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${node.pointsReward}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Node label
            Text(
                text = node.title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .width(100.dp)
                    .padding(top = Spacing.xs)
            )
        }
    }
}
