package com.ureka.play4change.features.explanation.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.UiError
import com.ureka.play4change.features.explanation.domain.model.ExplanationMessage

data class ExplanationState(
    override val isLoading: Boolean = true,
    override val error: UiError? = null,
    val sessionId: String = "",
    val status: String = "GENERATING",
    val explanationText: String? = null,
    val messages: List<ExplanationMessage> = emptyList(),
    val inputText: String = "",
    val showInput: Boolean = false,
    val isSending: Boolean = false,
    val isResolving: Boolean = false
) : ComponentState {
    val isGenerating: Boolean get() = status == "GENERATING"
    val isActive: Boolean get() = status == "ACTIVE"
}
