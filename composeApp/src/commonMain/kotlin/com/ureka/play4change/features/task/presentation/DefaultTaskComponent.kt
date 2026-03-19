package com.ureka.play4change.features.task.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.features.task.domain.model.TaskContent
import com.ureka.play4change.features.task.domain.repository.TaskRepository

class DefaultTaskComponent(
    componentContext: ComponentContext,
    private val userTaskId: String,
    private val repository: TaskRepository
) : BaseComponent<TaskState, TaskEvents>(componentContext, TaskState()), TaskComponent {

    init {
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
            // Legacy single-question events
            is TaskEvents.SelectOption -> {
                if (!state.value.submitted) {
                    updateState { copy(selectedIndex = event.index) }
                }
            }
            TaskEvents.ToggleHint -> updateState { copy(hintVisible = !hintVisible, showHint = !showHint) }
            TaskEvents.Submit     -> submit()

            // Quiz events
            is TaskEvents.SelectAnswer -> updateState {
                copy(answers = answers + (event.questionIndex to event.optionIndex))
            }
            TaskEvents.NextQuestion -> updateState {
                copy(currentQuestionIndex = currentQuestionIndex + 1)
            }
            TaskEvents.SubmitQuiz -> submitQuiz()

            // Step events
            TaskEvents.NextStep -> updateState {
                copy(currentStepIndex = currentStepIndex + 1,
                     stepsCompleted = stepsCompleted + currentStepIndex)
            }
            is TaskEvents.PhotoCaptured -> updateState { copy(capturedPhotoUri = event.uri) }
            TaskEvents.SubmitTask -> submitStepTask()

            // Shared
            TaskEvents.Continue      -> emitEffect(TaskEffect.NavigateBack)
            TaskEvents.ExitRequested -> emitEffect(TaskEffect.NavigateBack)
        }
    }

    private fun submit() {
        val selected = state.value.selectedIndex ?: return
        val taskId = state.value.task?.userTaskId ?: return
        safeLaunch(scope) {
            val result = repository.submitAnswer(taskId, selected)
            updateState {
                copy(
                    submitted = true,
                    isCorrect = result.isCorrect,
                    pointsAwarded = result.pointsAwarded,
                    submission = SubmissionState.Resolved(result.isCorrect)
                )
            }
        }
    }

    private fun submitQuiz() {
        val content = state.value.task?.content as? TaskContent.QuizContent ?: return
        val taskId = state.value.task?.userTaskId ?: return
        val score = content.questions.count { q ->
            state.value.answers[content.questions.indexOf(q)] == q.correctIndex
        }
        safeLaunch(scope) {
            val result = repository.submitAnswer(taskId, score)
            updateState {
                copy(
                    quizSubmitted = true,
                    quizScore = score,
                    pointsAwarded = result.pointsAwarded,
                    submission = SubmissionState.Resolved(result.isCorrect),
                    isCorrect = result.isCorrect,
                    submitted = true
                )
            }
        }
    }

    private fun submitStepTask() {
        val taskId = state.value.task?.userTaskId ?: return
        safeLaunch(scope) {
            val result = repository.submitAnswer(taskId, -1)
            updateState {
                copy(
                    submission = SubmissionState.Resolved(result.isCorrect),
                    pointsAwarded = result.pointsAwarded,
                    isCorrect = result.isCorrect,
                    submitted = true
                )
            }
        }
    }

    override fun TaskState.copyBase(isLoading: Boolean, error: AppError?): TaskState =
        copy(isLoading = isLoading, error = error)
}
