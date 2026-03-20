package com.ureka.play4change.features.task.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.ureka.play4change.core.BaseView
import com.ureka.play4change.core.camera.CameraSection
import com.ureka.play4change.design.Spacing
import com.ureka.play4change.design.components.OptionState
import com.ureka.play4change.design.components.ResultOverlay
import com.ureka.play4change.design.components.ShimmerBox
import com.ureka.play4change.design.components.TaskOptionButton
import com.ureka.play4change.features.task.domain.model.TaskContent
import com.ureka.play4change.features.task.presentation.DefaultTaskComponent
import com.ureka.play4change.features.task.presentation.SubmissionState
import com.ureka.play4change.features.task.presentation.TaskEvents
import com.ureka.play4change.features.task.presentation.TaskState
import org.jetbrains.compose.resources.stringResource
import play4change.composeapp.generated.resources.Res
import play4change.composeapp.generated.resources.task_continue
import play4change.composeapp.generated.resources.task_hide_hint
import play4change.composeapp.generated.resources.task_next_question
import play4change.composeapp.generated.resources.task_question_label
import play4change.composeapp.generated.resources.task_question_of
import play4change.composeapp.generated.resources.task_review_skipped
import play4change.composeapp.generated.resources.task_show_hint
import play4change.composeapp.generated.resources.task_skip_question
import play4change.composeapp.generated.resources.task_skipped_label
import play4change.composeapp.generated.resources.task_step_of
import play4change.composeapp.generated.resources.task_submit
import play4change.composeapp.generated.resources.task_submit_quiz
import play4change.composeapp.generated.resources.task_submit_task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(component: DefaultTaskComponent) {
    BaseView(
        component = component,
        topBar = {
            val taskState by component.state.subscribeAsState()
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = taskState.task?.title ?: stringResource(Res.string.task_question_label),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    if (!taskState.isBackBlocked) {
                        IconButton(onClick = { component.onEvent(TaskEvents.Continue) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { state, onEvent, innerPadding ->
        Box(Modifier.fillMaxSize()) {
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.l, vertical = Spacing.m),
                        verticalArrangement = Arrangement.spacedBy(Spacing.m)
                    ) {
                        when (val content = task.content) {
                            is TaskContent.QuizContent -> QuizModeContent(state, content, onEvent)
                            is TaskContent.StepContent -> StepModeContent(state, content, onEvent)
                            null -> LegacyQuizContent(state, task, onEvent)
                        }
                    }
                }
            }

            // Full-screen result overlay — fills the Box which starts at window origin,
            // covering topBar and all content when visible.
            val isResolved = state.submission is SubmissionState.Resolved
            ResultOverlay(
                visible = state.submitted || isResolved,
                isCorrect = state.isCorrect,
                pointsAwarded = state.pointsAwarded,
                onContinue = { onEvent(TaskEvents.Continue) }
            )
        }
    }
}

@Composable
private fun QuizModeContent(
    state: TaskState,
    content: TaskContent.QuizContent,
    onEvent: (TaskEvents) -> Unit
) {
    val q = content.questions[state.currentQuestionIndex]
    val totalQuestions = content.questions.size
    val selectedAnswer = state.answers[state.currentQuestionIndex]
    val isAnswered = selectedAnswer != null
    val isInReview = state.reviewQueue.isNotEmpty()
    val isLastInReview = isInReview && state.reviewQueuePosition == state.reviewQueue.size - 1
    val noMoreUnanswered = nextUnansweredOrNull(state, content) == null

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {

        // ── Progress bar ──────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            content.questions.forEachIndexed { i, _ ->
                val color = when {
                    i in state.answers       -> MaterialTheme.colorScheme.secondary
                    i in state.skippedIndices -> MaterialTheme.colorScheme.tertiary
                    i == state.currentQuestionIndex -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                    Modifier
                        .height(5.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // ── Question counter ──────────────────────────────────────────────
        Text(
            text = if (isInReview)
                stringResource(Res.string.task_skipped_label) +
                    " ${state.reviewQueuePosition + 1}/${state.reviewQueue.size}"
            else
                stringResource(
                    Res.string.task_question_of,
                    state.currentQuestionIndex + 1,
                    totalQuestions
                ),
            style = MaterialTheme.typography.labelMedium,
            color = if (isInReview) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ── Question card ──────────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(Spacing.l)) {
                Text(q.text, style = MaterialTheme.typography.titleMedium)

                // ── HINT — always has dedicated space, expands on tap ──────
                Spacer(Modifier.height(Spacing.m))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(Spacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = if (state.showHint) MaterialTheme.colorScheme.tertiary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = { onEvent(TaskEvents.ToggleHint) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (state.showHint) Res.string.task_hide_hint
                                else Res.string.task_show_hint
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Hint content — expands with animation
                AnimatedVisibility(
                    visible = state.showHint,
                    enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                    exit  = shrinkVertically(tween(200)) + fadeOut(tween(150))
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xs),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = state.task?.hint ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(Spacing.s)
                        )
                    }
                }
            }
        }

        // ── Options ────────────────────────────────────────────────────────
        q.options.forEachIndexed { i, option ->
            val optionState = when {
                state.autoAdvanceCountdown > 0 && selectedAnswer == i && state.lastAnswerCorrect == true
                    -> OptionState.Correct
                state.autoAdvanceCountdown > 0 && selectedAnswer == i && state.lastAnswerCorrect == false
                    -> OptionState.Wrong
                state.autoAdvanceCountdown > 0 && i == q.correctIndex && state.lastAnswerCorrect == false
                    -> OptionState.Correct
                selectedAnswer == i && state.autoAdvanceCountdown == 0
                    -> OptionState.Selected
                else -> OptionState.Idle
            }
            TaskOptionButton(
                text = option,
                optionState = optionState,
                onClick = {
                    if (!isAnswered && state.autoAdvanceCountdown == 0) {
                        onEvent(TaskEvents.SelectAnswer(state.currentQuestionIndex, i))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Bottom action row: Skip + Submit/Review ────────────────────────
        Spacer(Modifier.height(Spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            // Skip button — hidden when answered or in review mode
            if (!isAnswered && !isInReview && state.autoAdvanceCountdown == 0) {
                OutlinedButton(
                    onClick = { onEvent(TaskEvents.SkipQuestion) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(Res.string.task_skip_question))
                }
            }

            // Submit / Review skipped button
            val hasSkipped = state.skippedIndices.isNotEmpty()
            if (noMoreUnanswered || isInReview) {
                val buttonText = when {
                    isInReview && !isLastInReview -> stringResource(Res.string.task_next_question)
                    isInReview && isLastInReview  -> stringResource(Res.string.task_submit_quiz)
                    hasSkipped ->
                        stringResource(Res.string.task_review_skipped) +
                            " (${state.skippedIndices.size})"
                    else -> stringResource(Res.string.task_submit_quiz)
                }
                val isEnabled = when {
                    isInReview -> isAnswered && state.autoAdvanceCountdown == 0
                    else       -> isAnswered || (noMoreUnanswered && state.autoAdvanceCountdown == 0)
                }
                Button(
                    onClick = {
                        when {
                            isInReview && isLastInReview -> onEvent(TaskEvents.SubmitFinal)
                            else -> onEvent(TaskEvents.SubmitOrReview)
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = isEnabled,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(buttonText, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// Returns null when all questions are answered or skipped
private fun nextUnansweredOrNull(state: TaskState, content: TaskContent.QuizContent): Int? {
    return (0 until content.questions.size).firstOrNull {
        it !in state.answers && it !in state.skippedIndices
    }
}

@Composable
private fun StepModeContent(
    state: TaskState,
    content: TaskContent.StepContent,
    onEvent: (TaskEvents) -> Unit
) {
    val step = content.steps[state.currentStepIndex]
    val isLast = state.currentStepIndex == content.steps.size - 1

    LinearProgressIndicator(
        progress = { (state.currentStepIndex + 1f) / content.steps.size },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.secondaryContainer
    )

    Text(
        stringResource(Res.string.task_step_of, state.currentStepIndex + 1, content.steps.size),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.l)) {
            Text(step.instruction, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp)
        }
    }

    if (step.requiresPhoto) {
        Spacer(Modifier.height(Spacing.s))
        CameraSection(
            capturedUri = state.capturedPhotoUri,
            onCapture = { uri -> onEvent(TaskEvents.PhotoCaptured(uri)) }
        )
    }

    Spacer(Modifier.height(Spacing.s))
    if (!isLast) {
        Button(
            onClick = { onEvent(TaskEvents.NextStep) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.medium
        ) { Text(stringResource(Res.string.task_next_question)) }
    } else {
        Button(
            onClick = { onEvent(TaskEvents.SubmitTask) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = state.capturedPhotoUri != null,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) { Text(stringResource(Res.string.task_submit_task)) }
    }
}

@Composable
private fun LegacyQuizContent(
    state: TaskState,
    task: com.ureka.play4change.features.task.domain.model.TaskDetail,
    onEvent: (TaskEvents) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(Spacing.l)) {
            SuggestionChip(
                onClick = {},
                label = { Text(task.domain, style = MaterialTheme.typography.labelSmall) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = stringResource(Res.string.task_question_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.s))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            )

            AnimatedVisibility(task.hint.isNotBlank()) {
                Column {
                    Spacer(Modifier.height(Spacing.s))
                    TextButton(
                        onClick = { onEvent(TaskEvents.ToggleHint) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            if (state.hintVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Lightbulb,
                            null, Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(if (state.hintVisible) Res.string.task_hide_hint else Res.string.task_show_hint),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    AnimatedVisibility(state.hintVisible) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                task.hint,
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(Spacing.m)
                            )
                        }
                    }
                }
            }
        }
    }

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

    val submitState = when {
        state.submitted                                    -> "continue"
        state.selectedIndex != null && state.isLoading     -> "loading"
        state.selectedIndex != null                        -> "submit"
        else                                               -> "idle"
    }
    AnimatedContent(
        targetState = submitState,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "submit_anim"
    ) { btnState ->
        when (btnState) {
            "idle" -> OutlinedButton(
                onClick = {}, enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text(stringResource(Res.string.task_submit)) }

            "loading" -> Button(
                onClick = {}, enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                CircularProgressIndicator(
                    Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp
                )
            }

            "submit" -> Button(
                onClick = { onEvent(TaskEvents.Submit) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text(stringResource(Res.string.task_submit), style = MaterialTheme.typography.labelLarge) }

            else -> Button(
                onClick = { onEvent(TaskEvents.Continue) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isCorrect) MaterialTheme.colorScheme.secondary
                                     else MaterialTheme.colorScheme.primary
                )
            ) { Text(stringResource(Res.string.task_continue), style = MaterialTheme.typography.labelLarge) }
        }
    }
    Spacer(Modifier.height(Spacing.xxl))
}
