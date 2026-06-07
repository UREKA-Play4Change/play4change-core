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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val repository: HomeRepository,
    private val profileRepository: ProfileRepository
) : BaseComponent<HomeState, HomeEvents>(componentContext, HomeState()), HomeComponent {

    private var loadJob: Job? = null

    private fun loadHome() {
        loadJob?.cancel()
        updateState { copy(isLoading = true, networkError = null, error = null) }
        loadJob = scope.launch {
            try {
                val data = repository.getHomeData(userId = "current-user")
                updateState { copy(isLoading = false, homeData = data, showEnrollPrompt = !data.isEnrolled) }
                when {
                    // If any topic's task is still generating, poll until it resolves.
                    data.todayTasks.any { it.isGenerating } -> pollWhileGenerating()
                    // If any topic just completed and is waiting for the next task, poll until it appears.
                    data.todayTasks.any { it.isWaitingForNext } -> pollWhileWaiting()
                }
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

    // Poll every 4 s up to 20 attempts (~80 s) until no task is in generating state.
    private suspend fun pollWhileGenerating() {
        repeat(20) {
            delay(4000L)
            try {
                val data = repository.getHomeData(userId = "current-user")
                updateState { copy(homeData = data, showEnrollPrompt = !data.isEnrolled) }
                if (data.todayTasks.none { it.isGenerating }) return
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (_: Exception) {
                // transient error — keep polling
            }
        }
    }

    // Poll every 3 s up to 20 attempts (~60 s) until a new task becomes available.
    // Handles short dev-mode rate windows (e.g. 3 s) so users don't need to manually refresh.
    private suspend fun pollWhileWaiting() {
        repeat(20) {
            delay(3000L)
            try {
                val data = repository.getHomeData(userId = "current-user")
                updateState { copy(homeData = data, showEnrollPrompt = !data.isEnrolled) }
                // Stop when at least one topic has a pending task or generation in progress
                if (data.todayTasks.any { it.task != null || it.isGenerating }) return
                // Also stop if no topics are waiting any more
                if (data.todayTasks.none { it.isWaitingForNext }) return
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (_: Exception) {
                // transient error — keep polling
            }
        }
    }

    override fun onEvent(event: HomeEvents) {
        when (event) {
            is HomeEvents.StartTask          -> emitEffect(HomeEffect.NavigateToTask(event.userTaskId))
            is HomeEvents.ContinueStruggle   -> emitEffect(HomeEffect.NavigateToStruggle(event.enrollmentId))
            is HomeEvents.ContinueExplanation -> emitEffect(HomeEffect.NavigateToExplanation(event.sessionId))
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
