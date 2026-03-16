package com.ureka.play4change.features.task.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
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
            is TaskEvents.SelectOption -> {
                if (!state.value.submitted) {
                    updateState { copy(selectedIndex = event.index) }
                }
            }
            TaskEvents.ToggleHint    -> updateState { copy(hintVisible = !hintVisible) }
            TaskEvents.Submit        -> submit()
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
                    pointsAwarded = result.pointsAwarded
                )
            }
        }
    }

    override fun TaskState.copyBase(isLoading: Boolean, error: AppError?): TaskState =
        copy(isLoading = isLoading, error = error)
}
