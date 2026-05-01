package com.ureka.play4change.application.enrollment

import com.ureka.play4change.domain.topic.TaskTemplate

sealed class LanguageGatingResult {
    data class Available(val template: TaskTemplate) : LanguageGatingResult()
    data class Pending(val requestedLanguage: String) : LanguageGatingResult()
}
