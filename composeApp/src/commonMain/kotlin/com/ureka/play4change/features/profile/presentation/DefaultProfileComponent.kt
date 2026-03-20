package com.ureka.play4change.features.profile.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.features.profile.domain.repository.ProfileRepository

class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val repository: ProfileRepository
) : BaseComponent<ProfileState, ProfileEvents>(componentContext, ProfileState()), ProfileComponent {

    init {
        loadProfile()
    }

    private fun loadProfile() {
        safeLaunch(scope) {
            val profile = repository.getProfile("current-user")
            updateState { copy(isLoading = false, profile = profile) }
        }
    }

    override fun onEvent(event: ProfileEvents) {
        when (event) {
            ProfileEvents.NavigateBack -> emitEffect(ProfileEffect.NavigateBack)
        }
    }

    override fun ProfileState.copyBase(isLoading: Boolean, error: AppError?): ProfileState =
        copy(isLoading = isLoading, error = error)
}
