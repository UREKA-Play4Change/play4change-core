package com.ureka.play4change.features.home.presentation

import com.ureka.play4change.core.component.base.BaseComponent

sealed class HomeEffect : BaseComponent.Effect {
    data class NavigateToTask(val userTaskId: String) : HomeEffect()
    data object NavigateToProfile : HomeEffect()
    data object NavigateToAbout   : HomeEffect()
    data object NavigateToExplore : HomeEffect()
    data object LoggedOut         : HomeEffect()
}
