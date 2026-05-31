package com.ureka.play4change.features.struggle.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface StruggleEvents : ComponentEvents {
    data class SelectOption(val index: Int) : StruggleEvents
    data object Submit : StruggleEvents
    data object Continue : StruggleEvents
    data object RetryLoad : StruggleEvents
}
