package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.BadgeEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BadgeJpaRepository : JpaRepository<BadgeEntity, String> {

    @Query("SELECT b FROM BadgeEntity b WHERE b.userId = :userId AND b.microCompetence.id = :microCompetenceId")
    fun findByUserIdAndMicroCompetenceId(userId: String, microCompetenceId: String): BadgeEntity?

    fun findByUserId(userId: String): List<BadgeEntity>
    fun findByUserId(userId: String, pageable: Pageable): Page<BadgeEntity>

    @Query("SELECT b FROM BadgeEntity b WHERE b.microCompetence.id = :microCompetenceId")
    fun findByMicroCompetenceId(microCompetenceId: String): List<BadgeEntity>
}
