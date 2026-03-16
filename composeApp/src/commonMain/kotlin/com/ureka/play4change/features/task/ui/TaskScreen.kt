package com.ureka.play4change.features.task.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.OptionState
import com.ureka.play4change.design.components.ResultOverlay
import com.ureka.play4change.design.components.ShimmerBox
import com.ureka.play4change.design.components.TaskOptionButton
import com.ureka.play4change.features.task.presentation.DefaultTaskComponent
import com.ureka.play4change.features.task.presentation.TaskEffect
import com.ureka.play4change.features.task.presentation.TaskEvents
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.task_hide_hint
import play4change.composeapp.generated.resources.task_question_label
import play4change.composeapp.generated.resources.task_show_hint
import play4change.composeapp.generated.resources.task_submit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    component: DefaultTaskComponent,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(component) {
        component.effects.collect { effect ->
            when (effect as TaskEffect) {
                TaskEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    BaseView(component = component, screenAlignment = Alignment.TopStart) { state, onEvent ->
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = state.task?.title ?: stringResource(Res.string.task_question_label),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(TaskEvents.ExitRequested) }) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (state.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m)
                ) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(80.dp))
                    repeat(4) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp))
                    }
                }
            } else {
                val task = state.task
                if (task == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No task found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(Spacing.l),
                            verticalArrangement = Arrangement.spacedBy(Spacing.m)
                        ) {
                            // Question card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(Spacing.l)) {
                                    Text(
                                        text = stringResource(Res.string.task_question_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(Spacing.s))
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Hint
                            TextButton(
                                onClick = { onEvent(TaskEvents.ToggleHint) }
                            ) {
                                Text(
                                    text = if (state.hintVisible)
                                        stringResource(Res.string.task_hide_hint)
                                    else
                                        stringResource(Res.string.task_show_hint),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            if (state.hintVisible) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = task.hint,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(Spacing.l)
                                    )
                                }
                            }

                            // Options
                            task.options.forEachIndexed { index, option ->
                                val optionState = when {
                                    !state.submitted && state.selectedIndex == index -> OptionState.Selected
                                    state.submitted && index == task.correctIndex    -> OptionState.Correct
                                    state.submitted && state.selectedIndex == index &&
                                            index != task.correctIndex               -> OptionState.Wrong
                                    else                                             -> OptionState.Idle
                                }
                                TaskOptionButton(
                                    text = option,
                                    optionState = optionState,
                                    onClick = { onEvent(TaskEvents.SelectOption(index)) }
                                )
                            }

                            // Submit
                            if (!state.submitted) {
                                Button(
                                    onClick = { onEvent(TaskEvents.Submit) },
                                    enabled = state.selectedIndex != null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(Res.string.task_submit))
                                }
                            }

                            Spacer(Modifier.height(Spacing.xxl))
                        }

                        // Result overlay
                        ResultOverlay(
                            visible = state.submitted,
                            isCorrect = state.isCorrect,
                            pointsAwarded = state.pointsAwarded,
                            onContinue = { onEvent(TaskEvents.Continue) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(Spacing.l)
                        )
                    }
                }
            }
        }
    }
}
