package com.ureka.play4change.features.home.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.features.home.domain.repository.HomeRepository

class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val repository: HomeRepository
) : BaseComponent<HomeState, HomeEvents>(componentContext, HomeState()), HomeComponent {

    init {
        loadHome()
    }

    private fun loadHome() {
        safeLaunch(scope) {
            val data = repository.getHomeData(userId = "current-user")
            updateState { copy(isLoading = false, homeData = data) }
        }
    }

    override fun onEvent(event: HomeEvents) {
        when (event) {
            is HomeEvents.StartTask  -> emitEffect(HomeEffect.NavigateToTask(event.userTaskId))
            HomeEvents.OpenProfile   -> emitEffect(HomeEffect.NavigateToProfile)
            HomeEvents.OpenAbout     -> emitEffect(HomeEffect.NavigateToAbout)
        }
    }

    override fun HomeState.copyBase(isLoading: Boolean, error: AppError?): HomeState =
        copy(isLoading = isLoading, error = error)
}
