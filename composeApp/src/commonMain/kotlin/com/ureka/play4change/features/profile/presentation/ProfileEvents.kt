package com.ureka.play4change.features.profile.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface ProfileEvents : ComponentEvents {
    data object OpenAbout : ProfileEvents
    data object RequestSignOut : ProfileEvents
    data object ConfirmSignOut : ProfileEvents
    data object DismissSignOut : ProfileEvents
}
