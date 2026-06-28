package com.ureka.play4change.features.profile.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.UiError
import com.ureka.play4change.features.profile.domain.model.ProfileData
import com.ureka.play4change.features.profile.domain.model.RecoveryEmail

data class ProfileState(
    override val isLoading: Boolean = true,
    override val error: UiError? = null,
    val profile: ProfileData? = null,
    val isEditingName: Boolean = false,
    val nameInput: String = "",
    val isSavingName: Boolean = false,
    val languagePickerVisible: Boolean = false,
    val badgePage: Int = 0,
    val recoveryEmails: List<RecoveryEmail> = emptyList(),
    val isLoadingRecoveryEmails: Boolean = true,
    val recoveryEmailLoadFailed: Boolean = false,
    val addEmailDialogVisible: Boolean = false,
    val recoveryEmailInput: String = "",
    val isSavingRecoveryEmail: Boolean = false,
    val recoveryEmailDialogError: String? = null,
    val verifyDialogVisible: Boolean = false,
    val verifyTokenInput: String = "",
    val isVerifyingRecoveryEmail: Boolean = false,
    val verifyDialogError: String? = null
) : ComponentState
