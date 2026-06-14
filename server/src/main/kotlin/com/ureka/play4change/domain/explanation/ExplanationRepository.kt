package com.ureka.play4change.domain.explanation

interface ExplanationRepository {
    fun findById(id: String): ExplanationSession?
    fun findActiveByEnrollmentId(enrollmentId: String): ExplanationSession?
    fun findAllByEnrollmentId(enrollmentId: String): List<ExplanationSession>
    fun findAllByEnrollmentIdPaged(enrollmentId: String, page: Int, size: Int): com.ureka.play4change.domain.topic.PageResult<ExplanationSession>
    fun save(session: ExplanationSession): ExplanationSession
    fun saveMessage(message: ExplanationMessage): ExplanationMessage
    fun findMessagesBySessionId(sessionId: String): List<ExplanationMessage>
}
