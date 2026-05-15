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
            is ExploreEvents.RequestEnroll   -> updateState { copy(pendingEnroll = event.topic) }
            ExploreEvents.ConfirmEnroll      -> confirmEnroll()
            ExploreEvents.DismissEnroll      -> updateState { copy(pendingEnroll = null) }
            is ExploreEvents.RequestLeave    -> updateState { copy(pendingLeave = event.topic) }
            ExploreEvents.ConfirmLeave       -> confirmLeave()
            ExploreEvents.DismissLeave       -> updateState { copy(pendingLeave = null) }
            ExploreEvents.NavigateBack       -> emitEffect(ExploreEffect.NavigateBack)
        }
    }

    private fun confirmEnroll() {
        val topic = state.value.pendingEnroll ?: return
        safeLaunch(scope) {
            repository.enrollTopic("current-user", topic.id)
            val updated = state.value.topics.map { it.copy(isActive = it.id == topic.id) }
            updateState { copy(topics = updated, pendingEnroll = null, enrolled = true) }
            emitEffect(ExploreEffect.TopicEnrolled)
        }
    }

    private fun confirmLeave() {
        val topic = state.value.pendingLeave ?: return
        safeLaunch(scope) {
            repository.deactivateEnrollment("current-user", topic.id)
            val updated = state.value.topics.map { it.copy(isActive = false) }
            updateState { copy(topics = updated, pendingLeave = null) }
        }
    }

    override fun ExploreState.copyBase(isLoading: Boolean, error: AppError?): ExploreState =
        copy(isLoading = isLoading, error = error)
}
