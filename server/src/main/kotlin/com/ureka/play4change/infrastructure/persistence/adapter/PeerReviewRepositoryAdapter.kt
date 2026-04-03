package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.peerreview.*
import com.ureka.play4change.infrastructure.persistence.entity.PeerReviewEntity
import com.ureka.play4change.infrastructure.persistence.repository.PeerReviewJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TaskAssignmentJpaRepository
import org.springframework.stereotype.Component

@Component
class PeerReviewRepositoryAdapter(
    private val jpa: PeerReviewJpaRepository,
    private val assignmentJpa: TaskAssignmentJpaRepository
) : PeerReviewRepository {

    override fun findById(id: String): PeerReview? =
        jpa.findById(id).orElse(null)?.toDomain()

    override fun findBySubmissionAssignmentId(submissionAssignmentId: String): List<PeerReview> =
        jpa.findBySubmissionAssignmentId(submissionAssignmentId).map { it.toDomain() }

    override fun findPendingByReviewerUserId(reviewerUserId: String): List<PeerReview> =
        jpa.findByReviewerUserIdAndReviewedAtIsNull(reviewerUserId).map { it.toDomain() }

    override fun countBySubmissionAssignmentId(submissionAssignmentId: String): Int =
        jpa.countBySubmissionAssignmentId(submissionAssignmentId)

    override fun save(peerReview: PeerReview): PeerReview {
        val assignmentEntity = assignmentJpa.getReferenceById(peerReview.submissionAssignmentId)
        val entity = PeerReviewEntity(
            id = peerReview.id,
            submissionAssignment = assignmentEntity,
            reviewerUserId = peerReview.reviewerUserId,
            verdict = peerReview.verdict?.name,
            comment = peerReview.comment,
            assignedAt = peerReview.assignedAt,
            reviewedAt = peerReview.reviewedAt
        )
        return jpa.save(entity).toDomain()
    }

    override fun saveAll(reviews: List<PeerReview>): List<PeerReview> =
        reviews.map { save(it) }

    private fun PeerReviewEntity.toDomain(): PeerReview = PeerReview(
        id = id,
        submissionAssignmentId = submissionAssignment.id,
        reviewerUserId = reviewerUserId,
        verdict = verdict?.let { ReviewVerdict.valueOf(it) },
        comment = comment,
        assignedAt = assignedAt,
        reviewedAt = reviewedAt
    )
}
