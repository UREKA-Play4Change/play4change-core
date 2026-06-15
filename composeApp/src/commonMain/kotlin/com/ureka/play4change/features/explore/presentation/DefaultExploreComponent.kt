package com.ureka.play4change.features.explore.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.UiError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.features.explore.domain.model.EnrollmentStatus
import com.ureka.play4change.features.explore.domain.repository.ExploreRepository

class DefaultExploreComponent(
    componentContext: ComponentContext,
    private val repository: ExploreRepository,
    private val onNavigateBack: () -> Unit
) : BaseComponent<ExploreState, ExploreEvents>(componentContext, ExploreState()), ExploreComponent {

    init {
        loadTopics(0)
    }

    private fun loadTopics(page: Int) {
        safeLaunch(scope) {
            val result = repository.getTopics("current-user", page, PAGE_SIZE)
            updateState { copy(isLoading = false, topics = result.content, page = page, totalPages = result.totalPages) }
        }
    }

    override fun onEvent(event: ExploreEvents) {
        when (event) {
            ExploreEvents.LoadTopics         -> loadTopics(state.value.page)
            is ExploreEvents.SetFilter       -> updateState { copy(filter = event.filter) }
            is ExploreEvents.RequestEnroll   -> updateState { copy(pendingEnroll = event.topic) }
            ExploreEvents.ConfirmEnroll      -> confirmEnroll()
            ExploreEvents.DismissEnroll      -> updateState { copy(pendingEnroll = null) }
            is ExploreEvents.RequestLeave    -> updateState { copy(pendingLeave = event.topic) }
            ExploreEvents.ConfirmLeave       -> confirmLeave()
            ExploreEvents.DismissLeave       -> updateState { copy(pendingLeave = null) }
            ExploreEvents.NavigateBack       -> emitEffect(ExploreEffect.NavigateBack)
            ExploreEvents.NextPage           -> { val s = state.value; if (s.page < s.totalPages - 1) loadTopics(s.page + 1) }
            ExploreEvents.PreviousPage       -> { val s = state.value; if (s.page > 0) loadTopics(s.page - 1) }
        }
    }

    private fun confirmEnroll() {
        val topic = state.value.pendingEnroll ?: return
        safeLaunch(scope) {
            repository.enrollTopic("current-user", topic.id)
            val updated = state.value.topics.map { t ->
                if (t.id == topic.id) t.copy(enrollmentStatus = EnrollmentStatus.ACTIVE) else t
            }
            updateState { copy(topics = updated, pendingEnroll = null, enrolled = true) }
            emitEffect(ExploreEffect.TopicEnrolled)
        }
    }

    private fun confirmLeave() {
        val topic = state.value.pendingLeave ?: return
        safeLaunch(scope) {
            repository.deactivateEnrollment("current-user", topic.id)
            val updated = state.value.topics.map { t ->
                if (t.id == topic.id) t.copy(enrollmentStatus = EnrollmentStatus.PAUSED) else t
            }
            updateState { copy(topics = updated, pendingLeave = null) }
        }
    }

    override fun ExploreState.copyBase(isLoading: Boolean, error: UiError?): ExploreState =
        copy(isLoading = isLoading, error = error)

    private companion object {
        const val PAGE_SIZE = 5
    }
}
