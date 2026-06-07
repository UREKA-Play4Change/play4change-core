package com.ureka.play4change.features.explanation.domain.repository

import com.ureka.play4change.features.explanation.domain.model.ExplanationMessage
import com.ureka.play4change.features.explanation.domain.model.ExplanationSession

interface ExplanationRepository {
    suspend fun getSession(sessionId: String): ExplanationSession
    suspend fun sendMessage(sessionId: String, content: String): ExplanationMessage
    suspend fun resolve(sessionId: String)
}
