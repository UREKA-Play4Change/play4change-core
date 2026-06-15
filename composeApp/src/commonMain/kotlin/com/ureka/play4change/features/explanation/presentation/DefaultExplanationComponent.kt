package com.ureka.play4change.features.explanation.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.core.error.UiError
import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.toUiError
import com.ureka.play4change.core.network.toNetworkError
import com.ureka.play4change.features.explanation.domain.repository.ExplanationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class DefaultExplanationComponent(
    componentContext: ComponentContext,
    private val sessionId: String,
    private val repository: ExplanationRepository
) : BaseComponent<ExplanationState, ExplanationEvents>(componentContext, ExplanationState()), ExplanationComponent {

    init {
        loadSession()
    }

    private fun loadSession() {
        updateState { copy(isLoading = true, error = null) }
        scope.launch {
            try {
                val session = pollUntilActive()
                if (session == null) {
                    // AI generation timed out — go home so the learner is not stuck
                    emitEffect(ExplanationEffect.NavigateToHome)
                    return@launch
                }
                updateState {
                    copy(
                        isLoading = false,
                        sessionId = session.sessionId,
                        status = session.status,
                        explanationText = session.explanationText,
                        messages = session.messages
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

    /**
     * Poll every 3 s up to 20 attempts (~60 s) until the explanation has been generated
     * (status == ACTIVE). Returns null if the session never becomes active.
     */
    private suspend fun pollUntilActive(): com.ureka.play4change.features.explanation.domain.model.ExplanationSession? {
        repeat(20) {
            try {
                val session = repository.getSession(sessionId)
                if (session.status == "ACTIVE" || session.status == "RESOLVED") return session
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // transient error — keep polling
            }
            delay(3000L)
        }
        return null
    }

    override fun onEvent(event: ExplanationEvents) {
        when (event) {
            ExplanationEvents.Understood  -> resolve()
            ExplanationEvents.ToggleInput -> updateState { copy(showInput = !showInput, inputText = "") }
            is ExplanationEvents.InputChanged -> updateState { copy(inputText = event.text) }
            ExplanationEvents.SendMessage -> sendMessage()
            ExplanationEvents.RetryLoad   -> loadSession()
        }
    }

    private fun resolve() {
        updateState { copy(isResolving = true) }
        safeLaunch(scope) {
            repository.resolve(sessionId)
            emitEffect(ExplanationEffect.NavigateToHome)
        }
    }

    private fun sendMessage() {
        val text = state.value.inputText.trim()
        if (text.isBlank() || state.value.isSending) return
        updateState { copy(isSending = true, showInput = false, inputText = "") }
        safeLaunch(scope) {
            val aiReply = repository.sendMessage(sessionId, text)
            val updated = repository.getSession(sessionId)
            updateState {
                copy(
                    isSending = false,
                    messages = updated.messages
                )
            }
        }
    }

    override fun ExplanationState.copyBase(isLoading: Boolean, error: UiError?): ExplanationState =
        copy(isLoading = isLoading, error = error)
}
