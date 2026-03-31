package com.ureka.play4change.domain.struggle

interface StruggleRepository {
    fun findById(id: String): StruggleSession?
    fun findOpenByEnrollmentId(enrollmentId: String): StruggleSession?
    fun save(session: StruggleSession): StruggleSession
}
