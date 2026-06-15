package com.ureka.play4change.features.explore.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.UiError
import com.ureka.play4change.features.explore.domain.model.Topic

data class ExploreState(
    val topics: List<Topic> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val filter: ExploreFilter = ExploreFilter.ALL,
    val pendingEnroll: Topic? = null,
    val pendingLeave: Topic? = null,
    val enrolled: Boolean = false,
    override val isLoading: Boolean = true,
    override val error: UiError? = null
) : ComponentState
