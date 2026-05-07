package com.ureka.play4change.features.struggle.domain.repository

import com.ureka.play4change.features.struggle.domain.model.AdaptiveSubmitResult
import com.ureka.play4change.features.struggle.domain.model.StruggleSession

interface StruggleRepository {
    /** Returns null when there is no active struggle session (404 is not an error). */
    suspend fun getSession(enrollmentId: String): StruggleSession?
    suspend fun submitTask(sessionId: String, taskId: String, selectedOption: Int): AdaptiveSubmitResult
}
