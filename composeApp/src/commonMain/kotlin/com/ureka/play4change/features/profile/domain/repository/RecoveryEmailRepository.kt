package com.ureka.play4change.features.profile.domain.repository

import com.ureka.play4change.features.profile.domain.model.RecoveryEmail

interface RecoveryEmailRepository {
    suspend fun listRecoveryEmails(): List<RecoveryEmail>
    suspend fun addRecoveryEmail(email: String)
    suspend fun removeRecoveryEmail(id: String)
    suspend fun verifyRecoveryEmail(token: String)
}