package com.ureka.play4change.features.profile.data.mock

import com.ureka.play4change.features.profile.domain.model.RecoveryEmail
import com.ureka.play4change.features.profile.domain.repository.RecoveryEmailRepository
import kotlinx.coroutines.delay

class MockRecoveryEmailRepository : RecoveryEmailRepository {

    private val emails = mutableListOf(
        RecoveryEmail("1", "backup@example.com", true),
        RecoveryEmail("2", "pending@example.com", false)
    )
    private var nextId = 3

    override suspend fun listRecoveryEmails(): List<RecoveryEmail> {
        delay(300)
        return emails.toList()
    }

    override suspend fun addRecoveryEmail(email: String) {
        delay(400)
        emails.add(RecoveryEmail((nextId++).toString(), email, false))
    }

    override suspend fun removeRecoveryEmail(id: String) {
        delay(300)
        emails.removeAll { it.id == id }
    }

    override suspend fun verifyRecoveryEmail(token: String) {
        delay(300)
        val index = emails.indexOfFirst { !it.verified }
        if (index >= 0) emails[index] = emails[index].copy(verified = true)
    }
}