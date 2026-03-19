package com.ureka.play4change.features.explore.presentation

import com.ureka.play4change.core.component.base.ComponentEvents
import com.ureka.play4change.features.explore.domain.model.Topic

sealed interface ExploreEvents : ComponentEvents {
    data object LoadTopics : ExploreEvents
    data class RequestSwitch(val topic: Topic) : ExploreEvents
    data object ConfirmSwitch : ExploreEvents
    data object DismissSwitch : ExploreEvents
    data object NavigateBack : ExploreEvents
}
