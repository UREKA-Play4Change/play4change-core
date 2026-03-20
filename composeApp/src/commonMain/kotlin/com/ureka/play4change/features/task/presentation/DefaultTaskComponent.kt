package com.ureka.play4change.features.task.presentation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.features.task.domain.model.TaskContent
import com.ureka.play4change.features.task.domain.repository.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DefaultTaskComponent(
    componentContext: ComponentContext,
    private val userTaskId: String,
    private val repository: TaskRepository
) : BaseComponent<TaskState, TaskEvents>(componentContext, TaskState()), TaskComponent {

    // Registered while the task is active; swallows hardware back on all platforms.
    // Disabled (not unregistered) once the task is submitted so the user can navigate back.
    private val backCallback = BackCallback(isEnabled = true) { /* swallow — task is active */ }

    init {
        backHandler.register(backCallback)
        loadTask()
    }

    private fun loadTask() {
        safeLaunch(scope) {
            val task = repository.getTask(userTaskId)
            updateState { copy(isLoading = false, task = task) }
        }
    }

    override fun onEvent(event: TaskEvents) {
        when (event) {

            // ── Legacy single-question ─────────────────────────────────────────
            is TaskEvents.SelectOption -> {
                if (!state.value.submitted) {
                    updateState { copy(selectedIndex = event.index) }
                }
            }
            TaskEvents.ToggleHint -> updateState { copy(hintVisible = !hintVisible, showHint = !showHint) }
            TaskEvents.Submit     -> submitLegacy()

            // ── Quiz events ────────────────────────────────────────────────────
            is TaskEvents.SelectAnswer -> {
                val idx = state.value.currentQuestionIndex
                val content = state.value.task?.content as? TaskContent.QuizContent
                val isCorrect = event.optionIndex == content?.questions?.getOrNull(idx)?.correctIndex
                updateState {
                    copy(
                        answers = answers + (idx to event.optionIndex),
                        skippedIndices = skippedIndices - idx,   // un-skip if they answer
                        lastAnswerCorrect = isCorrect,
                        autoAdvanceCountdown = 3
                    )
                }
                startAutoAdvanceCountdown()
            }

            TaskEvents.SkipQuestion -> {
                val s = state.value
                val idx = s.currentQuestionIndex
                val total = (s.task?.content as? TaskContent.QuizContent)?.questions?.size ?: 0
                val newSkipped = s.skippedIndices + idx
                val next = (idx + 1 until total).firstOrNull { it !in s.answers && it !in newSkipped }
                updateState {
                    copy(
                        skippedIndices = newSkipped,
                        lastAnswerCorrect = null,
                        currentQuestionIndex = next ?: idx
                    )
                }
            }

            TaskEvents.AutoAdvance -> {
                val s = state.value
                updateState { copy(lastAnswerCorrect = null, autoAdvanceCountdown = 0) }
                // In review mode: clear banner only, user taps Next manually
                if (s.reviewQueue.isEmpty()) {
                    val total = (s.task?.content as? TaskContent.QuizContent)?.questions?.size ?: 0
                    val next = (s.currentQuestionIndex + 1 until total)
                        .firstOrNull { it !in s.answers && it !in s.skippedIndices }
                    if (next != null) {
                        updateState { copy(currentQuestionIndex = next) }
                    }
                }
            }

            TaskEvents.SubmitOrReview -> {
                val s = state.value
                when {
                    s.reviewQueue.isNotEmpty() -> {
                        // Already in review mode — advance to next skipped question
                        val nextPos = s.reviewQueuePosition + 1
                        if (nextPos < s.reviewQueue.size) {
                            updateState {
                                copy(
                                    reviewQueuePosition = nextPos,
                                    currentQuestionIndex = reviewQueue[nextPos],
                                    lastAnswerCorrect = null,
                                    autoAdvanceCountdown = 0
                                )
                            }
                        }
                    }
                    s.skippedIndices.isEmpty() -> submitQuiz()
                    else -> {
                        // Enter review mode — go to first skipped question
                        val queue = s.skippedIndices.sorted()
                        updateState {
                            copy(
                                reviewQueue = queue,
                                reviewQueuePosition = 0,
                                currentQuestionIndex = queue.first(),
                                lastAnswerCorrect = null
                            )
                        }
                    }
                }
            }

            TaskEvents.SubmitFinal -> submitQuiz()

            // ── Step events ────────────────────────────────────────────────────
            TaskEvents.NextStep -> updateState {
                copy(
                    currentStepIndex = currentStepIndex + 1,
                    stepsCompleted = stepsCompleted + currentStepIndex
                )
            }
            is TaskEvents.PhotoCaptured -> updateState { copy(capturedPhotoUri = event.uri) }
            TaskEvents.SubmitTask -> submitStepTask()

            // ── Shared ─────────────────────────────────────────────────────────
            TaskEvents.Continue -> {
                unlockBack()
                updateState { copy(isBackBlocked = false) }
                emitEffect(TaskEffect.NavigateBack)
            }
        }
    }

    /** Disables the back-swallowing callback so the user can navigate back after submission. */
    private fun unlockBack() {
        backCallback.isEnabled = false
    }

    private fun startAutoAdvanceCountdown() {
        scope.launch {
            for (i in 3 downTo 1) {
                updateState { copy(autoAdvanceCountdown = i) }
                delay(1000L)
            }
            updateState { copy(autoAdvanceCountdown = 0) }
            onEvent(TaskEvents.AutoAdvance)
        }
    }

    private fun submitLegacy() {
        val selected = state.value.selectedIndex ?: return
        val taskId = state.value.task?.userTaskId ?: return
        safeLaunch(scope) {
            val result = repository.submitAnswer(taskId, selected)
            unlockBack()
            updateState {
                copy(
                    submitted = true,
                    isCorrect = result.isCorrect,
                    pointsAwarded = result.pointsAwarded,
                    submission = SubmissionState.Resolved(result.isCorrect),
                    isBackBlocked = false
                )
            }
        }
    }

    private fun submitQuiz() {
        val s = state.value
        val content = s.task?.content as? TaskContent.QuizContent ?: return
        val taskId = s.task.userTaskId
        val score = content.questions.count { q ->
            s.answers[content.questions.indexOf(q)] == q.correctIndex
        }
        safeLaunch(scope) {
            val result = repository.submitAnswer(taskId, score)
            unlockBack()
            updateState {
                copy(
                    quizSubmitted = true,
                    quizScore = score,
                    pointsAwarded = result.pointsAwarded,
                    submission = SubmissionState.Resolved(result.isCorrect),
                    isCorrect = result.isCorrect,
                    submitted = true,
                    isBackBlocked = false
                )
            }
        }
    }

    private fun submitStepTask() {
        val taskId = state.value.task?.userTaskId ?: return
        safeLaunch(scope) {
            val result = repository.submitAnswer(taskId, -1)
            unlockBack()
            updateState {
                copy(
                    submission = SubmissionState.Resolved(result.isCorrect),
                    pointsAwarded = result.pointsAwarded,
                    isCorrect = result.isCorrect,
                    submitted = true,
                    isBackBlocked = false
                )
            }
        }
    }

    override fun TaskState.copyBase(isLoading: Boolean, error: AppError?): TaskState =
        copy(isLoading = isLoading, error = error)
}
