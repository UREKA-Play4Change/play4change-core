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
    data object ShowAddRecoveryEmailDialog : ProfileEvents
    data object DismissAddRecoveryEmailDialog : ProfileEvents
    data class RecoveryEmailInputChanged(val value: String) : ProfileEvents
    data object SubmitAddRecoveryEmail : ProfileEvents
    data class RemoveRecoveryEmail(val id: String) : ProfileEvents
    data object ShowVerifyRecoveryEmailDialog : ProfileEvents
    data object DismissVerifyRecoveryEmailDialog : ProfileEvents
    data class VerifyTokenInputChanged(val value: String) : ProfileEvents
    data object SubmitVerifyRecoveryEmail : ProfileEvents
}
