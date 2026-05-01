package com.ureka.play4change.application.port

interface LanguageGenerationPort {
    fun triggerGeneration(moduleId: String, dayIndex: Int, language: String)
}
