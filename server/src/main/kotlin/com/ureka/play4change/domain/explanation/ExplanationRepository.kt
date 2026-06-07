package com.ureka.play4change.domain.explanation

interface ExplanationRepository {
    fun findById(id: String): ExplanationSession?
    fun findActiveByEnrollmentId(enrollmentId: String): ExplanationSession?
    fun save(session: ExplanationSession): ExplanationSession
    fun saveMessage(message: ExplanationMessage): ExplanationMessage
    fun findMessagesBySessionId(sessionId: String): List<ExplanationMessage>
}
