package com.ureka.play4change.features.home.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.toAppError
import com.ureka.play4change.core.network.toNetworkError
import com.ureka.play4change.features.home.domain.repository.HomeRepository
import com.ureka.play4change.features.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val repository: HomeRepository,
    private val profileRepository: ProfileRepository
) : BaseComponent<HomeState, HomeEvents>(componentContext, HomeState()), HomeComponent {

    init {
        loadHome()
    }

    private fun loadHome() {
        updateState { copy(isLoading = true, networkError = null, error = null) }
        scope.launch {
            try {
                val data = repository.getHomeData(userId = "current-user")
                updateState { copy(isLoading = false, homeData = data, showEnrollPrompt = !data.isEnrolled) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: NetworkException) {
                updateState { copy(isLoading = false, networkError = e.error, error = e.error.toAppError()) }
            } catch (e: Exception) {
                val netError = e.toNetworkError()
                updateState { copy(isLoading = false, networkError = netError, error = netError.toAppError()) }
            }
        }
    }

    override fun onEvent(event: HomeEvents) {
        when (event) {
            is HomeEvents.StartTask  -> emitEffect(HomeEffect.NavigateToTask(event.userTaskId))
            HomeEvents.OpenProfile   -> emitEffect(HomeEffect.NavigateToProfile)
            HomeEvents.OpenAbout     -> emitEffect(HomeEffect.NavigateToAbout)
            HomeEvents.OpenExplore   -> emitEffect(HomeEffect.NavigateToExplore)
            HomeEvents.RequestLogOut -> updateState { copy(showLogOutDialog = true) }
            HomeEvents.DismissLogOut -> updateState { copy(showLogOutDialog = false) }
            HomeEvents.ConfirmLogOut -> safeLaunch(scope) {
                updateState { copy(showLogOutDialog = false) }
                profileRepository.signOut()
                emitEffect(HomeEffect.LoggedOut)
            }
            HomeEvents.RetryLoad            -> loadHome()
            HomeEvents.DismissEnrollPrompt  -> updateState { copy(showEnrollPrompt = false) }
        }
    }

    override fun HomeState.copyBase(isLoading: Boolean, error: AppError?): HomeState =
        copy(isLoading = isLoading, error = error)
}
