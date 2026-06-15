package com.ureka.play4change.features.profile.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.UiError
import com.ureka.play4change.features.profile.domain.model.ProfileData

data class ProfileState(
    override val isLoading: Boolean = true,
    override val error: UiError? = null,
    val profile: ProfileData? = null,
    val isEditingName: Boolean = false,
    val nameInput: String = "",
    val isSavingName: Boolean = false,
    val languagePickerVisible: Boolean = false,
    val badgePage: Int = 0
) : ComponentState
