package com.ureka.play4change.features.profile.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.UiError
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
            updateState { copy(isLoading = false, profile = profile, nameInput = profile.name, badgePage = 0) }
        }
    }

    override fun onEvent(event: ProfileEvents) {
        when (event) {
            ProfileEvents.NavigateBack -> emitEffect(ProfileEffect.NavigateBack)
            ProfileEvents.EditName -> updateState {
                copy(isEditingName = true, nameInput = profile?.name ?: "")
            }
            ProfileEvents.CancelEditName -> updateState {
                copy(isEditingName = false, nameInput = profile?.name ?: "")
            }
            is ProfileEvents.NameInputChanged -> updateState { copy(nameInput = event.value) }
            ProfileEvents.SaveName -> saveName()
            ProfileEvents.ShowLanguagePicker -> updateState { copy(languagePickerVisible = true) }
            ProfileEvents.DismissLanguagePicker -> updateState { copy(languagePickerVisible = false) }
            is ProfileEvents.LanguageSelected -> changeLanguage(event.code)
            ProfileEvents.NextBadgePage -> {
                val total = badgeTotalPages()
                if (state.value.badgePage < total - 1) updateState { copy(badgePage = badgePage + 1) }
            }
            ProfileEvents.PreviousBadgePage -> {
                if (state.value.badgePage > 0) updateState { copy(badgePage = badgePage - 1) }
            }
        }
    }

    private fun badgeTotalPages(): Int {
        val count = state.value.profile?.badges?.size ?: 0
        return maxOf(1, (count + BADGE_PAGE_SIZE - 1) / BADGE_PAGE_SIZE)
    }

    private fun saveName() {
        val name = state.value.nameInput.trim()
        if (name.length < 2) return
        safeLaunch(scope) {
            updateState { copy(isSavingName = true) }
            val updated = repository.updateName(name)
            updateState { copy(isSavingName = false, isEditingName = false, profile = updated) }
        }
    }

    private fun changeLanguage(code: String) {
        safeLaunch(scope) {
            repository.updatePreferences(code)
            updateState {
                copy(
                    languagePickerVisible = false,
                    profile = profile?.copy(preferredLanguage = code)
                )
            }
        }
    }

    override fun ProfileState.copyBase(isLoading: Boolean, error: UiError?): ProfileState =
        copy(isLoading = isLoading, error = error)

    private companion object {
        const val BADGE_PAGE_SIZE = 5
    }
}
