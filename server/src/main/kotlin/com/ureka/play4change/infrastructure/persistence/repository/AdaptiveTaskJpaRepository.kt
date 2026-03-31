package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.AdaptiveTaskEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AdaptiveTaskJpaRepository : JpaRepository<AdaptiveTaskEntity, String> {
    fun findByStruggleSessionIdOrderByOrderIndexAsc(struggleSessionId: String): List<AdaptiveTaskEntity>
}
