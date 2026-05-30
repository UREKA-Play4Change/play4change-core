package com.ureka.play4change.features.struggle.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.toAppError
import com.ureka.play4change.core.network.toNetworkError
import com.ureka.play4change.features.struggle.domain.repository.StruggleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class DefaultStruggleComponent(
    componentContext: ComponentContext,
    private val enrollmentId: String,
    private val repository: StruggleRepository
) : BaseComponent<StruggleState, StruggleEvents>(componentContext, StruggleState()), StruggleComponent {

    init {
        loadSession()
    }

    private fun loadSession() {
        updateState { copy(isLoading = true, error = null) }
        scope.launch {
            try {
                val session = repository.getSession(enrollmentId)
                if (session == null) {
                    // No active session — server may still be generating adaptive tasks.
                    // Retry once after a short delay.
                    delay(2000L)
                    val retried = repository.getSession(enrollmentId)
                    if (retried == null) {
                        emitEffect(StruggleEffect.NavigateToHome)
                        return@launch
                    }
                    updateState {
                        copy(
                            isLoading = false,
                            sessionId = retried.sessionId,
                            errorPattern = retried.errorPattern,
                            tasks = retried.tasks
                        )
                    }
                } else {
                    updateState {
                        copy(
                            isLoading = false,
                            sessionId = session.sessionId,
                            errorPattern = session.errorPattern,
                            tasks = session.tasks
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: NetworkException) {
                updateState { copy(isLoading = false, error = e.error.toAppError()) }
            } catch (e: Exception) {
                updateState { copy(isLoading = false, error = e.toNetworkError().toAppError()) }
            }
        }
    }

    override fun onEvent(event: StruggleEvents) {
        when (event) {
            is StruggleEvents.SelectOption -> {
                if (!state.value.submitted && !state.value.wrongAnswerFeedback) {
                    updateState { copy(selectedIndex = event.index) }
                }
            }
            StruggleEvents.Submit -> submitCurrent()
            StruggleEvents.Continue -> {
                val s = state.value
                if (s.sessionResolved) {
                    emitEffect(StruggleEffect.NavigateToHome)
                } else {
                    // Advance to next adaptive task
                    updateState {
                        copy(
                            currentIndex = currentIndex + 1,
                            selectedIndex = null,
                            submitted = false,
                            isCorrect = false,
                            pointsAwarded = 0
                        )
                    }
                }
            }
            StruggleEvents.RetryLoad -> loadSession()
        }
    }

    private fun submitCurrent() {
        val s = state.value
        val selected = s.selectedIndex ?: return
        val task = s.currentTask ?: return
        safeLaunch(scope) {
            val result = repository.submitTask(s.sessionId, task.taskId, selected)
            if (!result.isCorrect) {
                // Flash wrong and allow retry on the same adaptive task
                updateState { copy(wrongAnswerFeedback = true) }
                delay(1200L)
                updateState {
                    copy(
                        wrongAnswerFeedback = false,
                        selectedIndex = null
                    )
                }
            } else {
                updateState {
                    copy(
                        submitted = true,
                        isCorrect = true,
                        pointsAwarded = result.pointsAwarded,
                        sessionResolved = result.sessionResolved
                    )
                }
            }
        }
    }

    override fun StruggleState.copyBase(isLoading: Boolean, error: AppError?): StruggleState =
        copy(isLoading = isLoading, error = error)
}
