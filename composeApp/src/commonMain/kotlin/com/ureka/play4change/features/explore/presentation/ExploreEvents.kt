package com.ureka.play4change.features.explore.presentation

import com.ureka.play4change.core.component.base.ComponentEvents
import com.ureka.play4change.features.explore.domain.model.Topic

sealed interface ExploreEvents : ComponentEvents {
    data object LoadTopics : ExploreEvents
    data class RequestEnroll(val topic: Topic) : ExploreEvents
    data object ConfirmEnroll : ExploreEvents
    data object DismissEnroll : ExploreEvents
    data object NavigateBack : ExploreEvents
}
