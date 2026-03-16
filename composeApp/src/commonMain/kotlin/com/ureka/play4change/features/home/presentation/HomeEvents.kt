package com.ureka.play4change.features.home.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface HomeEvents : ComponentEvents {
    data class StartTask(val userTaskId: String) : HomeEvents
    data object OpenProfile : HomeEvents
    data object OpenAbout : HomeEvents
}
