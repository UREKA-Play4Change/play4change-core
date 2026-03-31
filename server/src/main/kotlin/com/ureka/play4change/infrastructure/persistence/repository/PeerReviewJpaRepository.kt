package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.PeerReviewEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PeerReviewJpaRepository : JpaRepository<PeerReviewEntity, String> {
    fun findBySubmissionAssignmentId(submissionAssignmentId: String): List<PeerReviewEntity>
    fun findByReviewerUserIdAndReviewedAtIsNull(reviewerUserId: String): List<PeerReviewEntity>
    fun countBySubmissionAssignmentId(submissionAssignmentId: String): Int
}
