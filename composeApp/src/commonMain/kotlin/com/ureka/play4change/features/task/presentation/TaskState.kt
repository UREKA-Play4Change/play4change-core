package com.ureka.play4change.features.task.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.features.task.domain.model.TaskDetail

sealed class SubmissionState {
    data object Idle : SubmissionState()
    data object Loading : SubmissionState()
    data class Resolved(val isCorrect: Boolean) : SubmissionState()
}

data class TaskState(
    override val isLoading: Boolean = true,
    override val error: AppError? = null,
    val task: TaskDetail? = null,

    // Quiz mode state
    val currentQuestionIndex: Int = 0,
    val answers: Map<Int, Int> = emptyMap(),          // questionIndex → selectedOptionIndex
    val skippedIndices: Set<Int> = emptySet(),         // questions the user skipped
    val reviewQueue: List<Int> = emptyList(),          // skipped indices in order, used after submit attempt
    val reviewQueuePosition: Int = 0,                  // which skipped question we're on in review
    val quizSubmitted: Boolean = false,
    val quizScore: Int = 0,

    // Result display
    val lastAnswerCorrect: Boolean? = null,            // null = not yet answered
    val autoAdvanceCountdown: Int = 0,                 // counts down 3..1 before moving on

    // Step mode state
    val currentStepIndex: Int = 0,
    val capturedPhotoUri: String? = null,
    val stepsCompleted: Set<Int> = emptySet(),

    // Legacy single-question support
    val selectedIndex: Int? = null,
    val hintVisible: Boolean = false,
    val submitted: Boolean = false,
    val isCorrect: Boolean = false,

    // Shared
    val submission: SubmissionState = SubmissionState.Idle,
    val pointsAwarded: Int = 0,
    val showHint: Boolean = false,

    // Back navigation is BLOCKED during an active quiz/task.
    // Unlocked after submission via Continue.
    val isBackBlocked: Boolean = true
) : ComponentState
