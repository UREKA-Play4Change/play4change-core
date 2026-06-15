package com.ureka.play4change.features.profile.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface ProfileEvents : ComponentEvents {
    data object NavigateBack : ProfileEvents
    data object EditName : ProfileEvents
    data object CancelEditName : ProfileEvents
    data class NameInputChanged(val value: String) : ProfileEvents
    data object SaveName : ProfileEvents
    data object ShowLanguagePicker : ProfileEvents
    data object DismissLanguagePicker : ProfileEvents
    data class LanguageSelected(val code: String) : ProfileEvents
    data object NextBadgePage : ProfileEvents
    data object PreviousBadgePage : ProfileEvents
}
