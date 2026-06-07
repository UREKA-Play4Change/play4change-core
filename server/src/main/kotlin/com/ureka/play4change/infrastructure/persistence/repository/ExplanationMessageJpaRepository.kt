package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.ExplanationMessageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ExplanationMessageJpaRepository : JpaRepository<ExplanationMessageEntity, String> {
    fun findBySessionIdOrderBySentAtAsc(sessionId: String): List<ExplanationMessageEntity>
}
