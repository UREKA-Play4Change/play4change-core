package com.ureka.play4change.features.profile.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.features.profile.domain.model.ProfileData

data class ProfileState(
    override val isLoading: Boolean = true,
    override val error: AppError? = null,
    val profile: ProfileData? = null
) : ComponentState
