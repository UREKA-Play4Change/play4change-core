package com.ureka.play4change.features.about.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError

class DefaultAboutComponent(
    componentContext: ComponentContext
) : BaseComponent<AboutState, AboutEvents>(componentContext, AboutState()), AboutComponent {

    override fun onEvent(event: AboutEvents) {
        when (event) {
            AboutEvents.NavigateBack -> emitEffect(AboutEffect.NavigateBack)
        }
    }

    override fun AboutState.copyBase(isLoading: Boolean, error: AppError?): AboutState =
        copy(isLoading = isLoading, error = error)
}
