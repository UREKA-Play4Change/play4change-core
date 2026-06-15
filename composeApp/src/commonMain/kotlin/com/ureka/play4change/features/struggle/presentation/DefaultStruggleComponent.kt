package com.ureka.play4change.features.struggle.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.UiError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.toUiError
import com.ureka.play4change.core.network.toNetworkError
import com.ureka.play4change.core.network.NetworkError
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
                // AI generation can take up to 60 s — poll until the session has tasks.
                val session = pollUntilReady()
                if (session == null) {
                    emitEffect(StruggleEffect.NavigateToHome)
                    return@launch
                }
                // Restore position after a cold restart: skip already-completed tasks so
                // the user continues from where they left off instead of task 0.
                val resumeIndex = session.tasks.indexOfFirst { !it.isCompleted }
                    .takeIf { it >= 0 } ?: 0
                updateState {
                    copy(
                        isLoading = false,
                        sessionId = session.sessionId,
                        errorPattern = session.errorPattern,
                        tasks = session.tasks,
                        currentIndex = resumeIndex
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: NetworkException) {
                updateState { copy(isLoading = false, error = e.error.toUiError()) }
            } catch (e: Exception) {
                updateState { copy(isLoading = false, error = e.toNetworkError().toUiError()) }
            }
        }
    }

    // Poll every 3 s, up to 20 attempts (~60 s total), waiting for the session to have tasks.
    // Transient network errors are swallowed and retried; only auth errors escape immediately.
    // Returns null if the session never becomes ready (AI timed out / abandoned).
    private suspend fun pollUntilReady(): com.ureka.play4change.features.struggle.domain.model.StruggleSession? {
        repeat(20) {
            try {
                val session = repository.getSession(enrollmentId)
                if (session != null && session.tasks.isNotEmpty()) return session
            } catch (e: CancellationException) {
                throw e
            } catch (e: NetworkException) {
                // Auth failures are permanent — propagate immediately.
                if (e.error is NetworkError.Unauthorized || e.error is NetworkError.Forbidden) throw e
                // Everything else (connection drop, timeout, 5xx) is transient — keep polling.
            } catch (_: Exception) {
                // Serialization or other transient error — keep polling.
            }
            delay(3000L)
        }
        return null
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
                    val explanationId = s.pendingExplanationSessionId
                    if (explanationId != null) {
                        emitEffect(StruggleEffect.NavigateToExplanation(explanationId))
                    } else {
                        emitEffect(StruggleEffect.NavigateToHome)
                    }
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
            // Adaptive tasks are one-shot — no retry regardless of correctness.
            if (!result.isCorrect) {
                // Brief wrong-answer flash, then lock the task and show the Continue overlay.
                updateState { copy(wrongAnswerFeedback = true) }
                delay(1200L)
                updateState {
                    copy(
                        wrongAnswerFeedback = false,
                        submitted = true,
                        isCorrect = false,
                        pointsAwarded = 0,
                        sessionResolved = result.sessionResolved,
                        pendingExplanationSessionId = result.explanationSessionId
                    )
                }
            } else {
                updateState {
                    copy(
                        submitted = true,
                        isCorrect = true,
                        pointsAwarded = 0, // adaptive tasks never award score points
                        sessionResolved = result.sessionResolved,
                        pendingExplanationSessionId = result.explanationSessionId
                    )
                }
            }
        }
    }

    override fun StruggleState.copyBase(isLoading: Boolean, error: UiError?): StruggleState =
        copy(isLoading = isLoading, error = error)
}
