package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.EnrollmentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EnrollmentJpaRepository : JpaRepository<EnrollmentEntity, String> {
    fun findByUserIdAndTopicId(userId: String, topicId: String): EnrollmentEntity?
    fun findByUserIdAndStatus(userId: String, status: String): List<EnrollmentEntity>
}
