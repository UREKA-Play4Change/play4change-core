package com.ureka.play4change.features.explore.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.features.explore.domain.repository.ExploreRepository

class DefaultExploreComponent(
    componentContext: ComponentContext,
    private val repository: ExploreRepository,
    private val onNavigateBack: () -> Unit
) : BaseComponent<ExploreState, ExploreEvents>(componentContext, ExploreState()), ExploreComponent {

    init {
        loadTopics()
    }

    private fun loadTopics() {
        safeLaunch(scope) {
            val topics = repository.getTopics("current-user")
            updateState { copy(isLoading = false, topics = topics) }
        }
    }

    override fun onEvent(event: ExploreEvents) {
        when (event) {
            ExploreEvents.LoadTopics         -> loadTopics()
            is ExploreEvents.RequestSwitch   -> updateState { copy(pendingSwitch = event.topic) }
            ExploreEvents.ConfirmSwitch      -> confirmSwitch()
            ExploreEvents.DismissSwitch      -> updateState { copy(pendingSwitch = null) }
            ExploreEvents.NavigateBack       -> emitEffect(ExploreEffect.NavigateBack)
        }
    }

    private fun confirmSwitch() {
        val topic = state.value.pendingSwitch ?: return
        safeLaunch(scope) {
            repository.switchTopic("current-user", topic.id)
            val updated = state.value.topics.map { it.copy(isActive = it.id == topic.id) }
            updateState { copy(topics = updated, pendingSwitch = null, switched = true) }
            emitEffect(ExploreEffect.TopicSwitched)
        }
    }

    override fun ExploreState.copyBase(isLoading: Boolean, error: AppError?): ExploreState =
        copy(isLoading = isLoading, error = error)
}
