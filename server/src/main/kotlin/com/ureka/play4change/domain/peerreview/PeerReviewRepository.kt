package com.ureka.play4change.domain.peerreview

interface PeerReviewRepository {
    fun findBySubmissionAssignmentId(submissionAssignmentId: String): List<PeerReview>
    fun findPendingByReviewerUserId(reviewerUserId: String): List<PeerReview>
    fun countBySubmissionAssignmentId(submissionAssignmentId: String): Int
    fun save(peerReview: PeerReview): PeerReview
    fun saveAll(reviews: List<PeerReview>): List<PeerReview>
}
