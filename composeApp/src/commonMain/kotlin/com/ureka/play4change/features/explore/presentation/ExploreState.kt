package com.ureka.play4change.features.explore.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.features.explore.domain.model.Topic

data class ExploreState(
    val topics: List<Topic> = emptyList(),
    val pendingSwitch: Topic? = null,
    val switched: Boolean = false,
    override val isLoading: Boolean = true,
    override val error: AppError? = null
) : ComponentState
