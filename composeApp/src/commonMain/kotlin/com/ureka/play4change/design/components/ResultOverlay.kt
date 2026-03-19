package com.ureka.play4change.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ureka.play4change.design.Spacing
import kotlin.random.Random
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.task_continue
import play4change.composeapp.generated.resources.task_result_correct
import play4change.composeapp.generated.resources.task_result_wrong

@Composable
fun ResultOverlay(
    visible: Boolean,
    isCorrect: Boolean,
    pointsAwarded: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = tween(400, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
        ),
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (isCorrect) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCorrect) ConfettiLayer()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(Spacing.xxl)
            ) {
                AnimatedCheckmark(isCorrect)

                Spacer(Modifier.height(Spacing.xl))

                Text(
                    text = if (isCorrect) stringResource(Res.string.task_result_correct)
                           else stringResource(Res.string.task_result_wrong),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isCorrect) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )

                if (isCorrect && pointsAwarded > 0) {
                    Spacer(Modifier.height(Spacing.m))
                    val animatedPoints by animateIntAsState(pointsAwarded, tween(800), label = "pts")
                    Text(
                        "+$animatedPoints pts",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(Spacing.xxl))

                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCorrect) MaterialTheme.colorScheme.secondary
                                         else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(Res.string.task_continue), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun AnimatedCheckmark(isCorrect: Boolean) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(600, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "checkmark"
    )
    val color = if (isCorrect) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

    Canvas(Modifier.size(80.dp)) {
        val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color.copy(alpha = 0.15f))
        drawCircle(color, style = stroke)

        if (progress > 0f) {
            val path = Path()
            if (isCorrect) {
                val startX = size.width * 0.25f; val startY = size.height * 0.5f
                val midX   = size.width * 0.45f; val midY   = size.height * 0.65f
                val endX   = size.width * 0.75f; val endY   = size.height * 0.35f
                if (progress < 0.5f) {
                    val t = progress / 0.5f
                    path.moveTo(startX, startY)
                    path.lineTo(startX + (midX - startX) * t, startY + (midY - startY) * t)
                } else {
                    val t = (progress - 0.5f) / 0.5f
                    path.moveTo(startX, startY); path.lineTo(midX, midY)
                    path.lineTo(midX + (endX - midX) * t, midY + (endY - midY) * t)
                }
            } else {
                val pad = size.width * 0.25f
                val p1 = progress.coerceAtMost(0.5f) / 0.5f
                val p2 = ((progress - 0.5f).coerceAtLeast(0f)) / 0.5f
                path.moveTo(pad, pad)
                path.lineTo(pad + (size.width - 2 * pad) * p1, pad + (size.height - 2 * pad) * p1)
                if (p2 > 0f) {
                    path.moveTo(size.width - pad, pad)
                    path.lineTo(
                        size.width - pad - (size.width - 2 * pad) * p2,
                        pad + (size.height - 2 * pad) * p2
                    )
                }
            }
            drawPath(path, color, style = stroke)
        }
    }
}

@Composable
private fun ConfettiLayer() {
    val particles = remember {
        List(40) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) }
    }
    val anim = rememberInfiniteTransition(label = "confetti")
    val time by anim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "time"
    )
    val confettiColors = listOf(
        Color(0xFF6C5CB8), Color(0xFF3DAA7D), Color(0xFFBA7517),
        Color(0xFFE24B4A), Color(0xFF3B8BD4)
    )
    Canvas(Modifier.fillMaxSize()) {
        particles.forEachIndexed { i, (x, startY, delay) ->
            val t = ((time + delay) % 1f)
            val y = (startY + t * 1.5f) % 1f
            drawCircle(
                color = confettiColors[i % confettiColors.size].copy(alpha = 1f - t),
                radius = 5.dp.toPx(),
                center = Offset(x * size.width, y * size.height)
            )
        }
    }
}
