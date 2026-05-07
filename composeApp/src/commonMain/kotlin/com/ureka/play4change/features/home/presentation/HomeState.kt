package com.ureka.play4change.features.home.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.network.NetworkError
import com.ureka.play4change.features.home.domain.model.HomeData

data class HomeState(
    override val isLoading: Boolean = true,
    override val error: AppError? = null,
    val homeData: HomeData? = null,
    val showLogOutDialog: Boolean = false,
    val networkError: NetworkError? = null
) : ComponentState
