package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.MicroCompetenceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MicroCompetenceJpaRepository : JpaRepository<MicroCompetenceEntity, String> {
    fun findByTopicId(topicId: String): MicroCompetenceEntity?
}
