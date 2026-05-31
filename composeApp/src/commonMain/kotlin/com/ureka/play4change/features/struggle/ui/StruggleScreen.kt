package com.ureka.play4change.features.struggle.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.OptionState
import com.ureka.play4change.design.components.ResultOverlay
import com.ureka.play4change.design.components.TaskOptionButton
import com.ureka.play4change.features.struggle.presentation.DefaultStruggleComponent
import com.ureka.play4change.features.struggle.presentation.StruggleEvents
import com.ureka.play4change.features.struggle.presentation.StruggleState
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StruggleScreen(component: DefaultStruggleComponent) {
    BaseView(
        component = component,
        onRetry = { component.onEvent(StruggleEvents.RetryLoad) },
        topBar = {
            val s by component.state.subscribeAsState()
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Consolidation Path",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    if (s.tasks.isNotEmpty()) {
                        Text(
                            text = "${s.currentIndex + 1} / ${s.tasks.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = Spacing.m)
                        )
                    }
                }
            )
        }
    ) { state, onEvent, innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(Spacing.m))
                Text(
                    text = "Preparing your personalised tasks…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else if (state.tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No tasks available.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            StruggleContent(state, onEvent, innerPadding)
        }

        // Full-screen result overlay after a correct answer
        ResultOverlay(
            visible = state.submitted,
            isCorrect = true,
            pointsAwarded = state.pointsAwarded,
            totalPoints = 0,
            struggleTriggered = false,
            onContinue = { onEvent(StruggleEvents.Continue) }
        )
    }
}

@Composable
private fun StruggleContent(
    state: StruggleState,
    onEvent: (StruggleEvents) -> Unit,
    innerPadding: PaddingValues
) {
    val task = state.currentTask ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m)
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = { (state.currentIndex + 1f) / state.tasks.size },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.tertiaryContainer
        )

        // Pattern label
        Text(
            text = patternLabel(state.errorPattern),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )

        // Question card
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(Spacing.l)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )
                if (task.hint.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.s))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(Spacing.xs))
                    TextButton(
                        onClick = { /* hint always visible in struggle mode */ },
                        contentPadding = PaddingValues(0.dp),
                        enabled = false
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Hint",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = task.hint,
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(Spacing.s)
                            )
                        }
                    }
                }
            }
        }

        // Options
        task.options.forEachIndexed { index, option ->
            val optionState = when {
                state.wrongAnswerFeedback && state.selectedIndex == index -> OptionState.Wrong
                state.selectedIndex == index -> OptionState.Selected
                else -> OptionState.Idle
            }
            TaskOptionButton(
                text = option,
                optionState = optionState,
                onClick = {
                    if (!state.wrongAnswerFeedback && !state.submitted) {
                        onEvent(StruggleEvents.SelectOption(index))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Submit button
        Spacer(Modifier.height(Spacing.xs))
        val canSubmit = state.selectedIndex != null && !state.wrongAnswerFeedback && !state.submitted
        Button(
            onClick = { onEvent(StruggleEvents.Submit) },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            if (state.wrongAnswerFeedback) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onTertiary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Submit", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
    }
}

private fun patternLabel(pattern: String): String = when (pattern) {
    "WRONG_CONCEPT"        -> "Concept clarification"
    "PARTIAL_UNDERSTANDING" -> "Deepening understanding"
    "READING_ERROR"        -> "Reading comprehension"
    "TIME_PRESSURE"        -> "Practice under pressure"
    else                   -> "Personalised practice"
}
