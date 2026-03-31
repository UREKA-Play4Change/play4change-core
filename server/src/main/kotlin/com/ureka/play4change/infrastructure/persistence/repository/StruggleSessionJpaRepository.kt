package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.StruggleSessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StruggleSessionJpaRepository : JpaRepository<StruggleSessionEntity, String> {
    fun findByEnrollmentIdAndStatus(enrollmentId: String, status: String): StruggleSessionEntity?
}
