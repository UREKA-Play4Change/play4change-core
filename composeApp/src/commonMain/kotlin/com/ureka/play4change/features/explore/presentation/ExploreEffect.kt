package com.ureka.play4change.features.explore.presentation

import com.ureka.play4change.core.component.base.BaseComponent

sealed class ExploreEffect : BaseComponent.Effect {
    data object NavigateBack : ExploreEffect()
    data object TopicSwitched : ExploreEffect()
}
