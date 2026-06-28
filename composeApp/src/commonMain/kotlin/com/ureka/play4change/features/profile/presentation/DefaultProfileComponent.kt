package com.ureka.play4change.features.profile.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.UiError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.core.network.NetworkError
import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.toNetworkError
import com.ureka.play4change.core.network.toUiError
import com.ureka.play4change.features.profile.domain.repository.ProfileRepository
import com.ureka.play4change.features.profile.domain.repository.RecoveryEmailRepository
import kotlinx.coroutines.launch

class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val repository: ProfileRepository,
    private val recoveryEmailRepository: RecoveryEmailRepository
) : BaseComponent<ProfileState, ProfileEvents>(componentContext, ProfileState()), ProfileComponent {

    init {
        loadProfile()
        loadRecoveryEmails()
    }

    private fun loadProfile() {
        safeLaunch(scope) {
            val profile = repository.getProfile("current-user")
            updateState { copy(isLoading = false, profile = profile, nameInput = profile.name, badgePage = 0) }
        }
    }

    private fun loadRecoveryEmails() {
        updateState { copy(isLoadingRecoveryEmails = true, recoveryEmailLoadFailed = false) }
        scope.launch {
            try {
                val emails = recoveryEmailRepository.listRecoveryEmails()
                updateState { copy(recoveryEmails = emails, isLoadingRecoveryEmails = false) }
            } catch (_: Exception) {
                updateState { copy(isLoadingRecoveryEmails = false, recoveryEmailLoadFailed = true) }
            }
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
            ProfileEvents.ShowAddRecoveryEmailDialog -> updateState {
                copy(addEmailDialogVisible = true, recoveryEmailInput = "")
            }
            ProfileEvents.DismissAddRecoveryEmailDialog -> updateState {
                copy(addEmailDialogVisible = false, recoveryEmailInput = "", recoveryEmailDialogError = null)
            }
            is ProfileEvents.RecoveryEmailInputChanged -> updateState {
                copy(recoveryEmailInput = event.value, recoveryEmailDialogError = null)
            }
            ProfileEvents.SubmitAddRecoveryEmail -> submitAddRecoveryEmail()
            is ProfileEvents.RemoveRecoveryEmail -> removeRecoveryEmail(event.id)
            ProfileEvents.ShowVerifyRecoveryEmailDialog -> updateState {
                copy(verifyDialogVisible = true, verifyTokenInput = "", verifyDialogError = null)
            }
            ProfileEvents.DismissVerifyRecoveryEmailDialog -> updateState {
                copy(verifyDialogVisible = false, verifyTokenInput = "", verifyDialogError = null)
            }
            is ProfileEvents.VerifyTokenInputChanged -> updateState {
                copy(verifyTokenInput = event.value, verifyDialogError = null)
            }
            ProfileEvents.SubmitVerifyRecoveryEmail -> submitVerifyRecoveryEmail()
        }
    }

    private fun submitAddRecoveryEmail() {
        val email = state.value.recoveryEmailInput.trim()
        if (email.isBlank()) return
        scope.launch {
            updateState { copy(isSavingRecoveryEmail = true, recoveryEmailDialogError = null) }
            try {
                recoveryEmailRepository.addRecoveryEmail(email)
                updateState { copy(isSavingRecoveryEmail = false, addEmailDialogVisible = false, recoveryEmailInput = "", recoveryEmailDialogError = null) }
                loadRecoveryEmails()
            } catch (e: NetworkException) {
                val networkError = e.error
                if (networkError is NetworkError.Unknown) {
                    // Server returned a business-rule error (400) with a user-readable message:
                    // keep the dialog open and show it inline so the user can correct the input.
                    updateState { copy(isSavingRecoveryEmail = false, recoveryEmailDialogError = networkError.message) }
                } else {
                    updateState { copy(isSavingRecoveryEmail = false, error = networkError.toUiError()) }
                }
            } catch (e: Exception) {
                updateState { copy(isSavingRecoveryEmail = false, error = e.toNetworkError().toUiError()) }
            }
        }
    }

    private fun submitVerifyRecoveryEmail() {
        val token = state.value.verifyTokenInput.trim()
        if (token.isBlank()) return
        scope.launch {
            updateState { copy(isVerifyingRecoveryEmail = true, verifyDialogError = null) }
            try {
                recoveryEmailRepository.verifyRecoveryEmail(token)
                updateState { copy(isVerifyingRecoveryEmail = false, verifyDialogVisible = false, verifyTokenInput = "") }
                loadRecoveryEmails()
            } catch (e: NetworkException) {
                val networkError = e.error
                if (networkError is NetworkError.Unknown) {
                    updateState { copy(isVerifyingRecoveryEmail = false, verifyDialogError = networkError.message) }
                } else {
                    updateState { copy(isVerifyingRecoveryEmail = false, error = networkError.toUiError()) }
                }
            } catch (e: Exception) {
                updateState { copy(isVerifyingRecoveryEmail = false, error = e.toNetworkError().toUiError()) }
            }
        }
    }

    private fun removeRecoveryEmail(id: String) {
        scope.launch {
            try {
                recoveryEmailRepository.removeRecoveryEmail(id)
                loadRecoveryEmails()
            } catch (e: NetworkException) {
                updateState { copy(error = e.error.toUiError()) }
            } catch (e: Exception) {
                updateState { copy(error = e.toNetworkError().toUiError()) }
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
