package com.ureka.play4change.features.about.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface AboutEvents : ComponentEvents {
    data object NavigateBack : AboutEvents
}
