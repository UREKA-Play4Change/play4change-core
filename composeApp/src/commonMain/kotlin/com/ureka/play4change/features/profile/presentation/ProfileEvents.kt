package com.ureka.play4change.features.profile.presentation

import com.ureka.play4change.core.component.base.ComponentEvents
import com.ureka.play4change.features.about.presentation.AboutEvents

sealed interface ProfileEvents : ComponentEvents {
    data object NavigateBack : ProfileEvents
    data object OpenAbout : ProfileEvents
    data object RequestSignOut : ProfileEvents
    data object ConfirmSignOut : ProfileEvents
    data object DismissSignOut : ProfileEvents
}
