package com.ureka.play4change.domain.peerreview

interface PeerReviewRepository {
    fun findById(id: String): PeerReview?
    fun findBySubmissionAssignmentId(submissionAssignmentId: String): List<PeerReview>
    fun findBySubmissionAssignmentIdIn(submissionAssignmentIds: List<String>): List<PeerReview>
    fun findPendingByReviewerUserId(reviewerUserId: String): List<PeerReview>
    fun findExpiredPending(now: java.time.OffsetDateTime): List<PeerReview>
    fun countBySubmissionAssignmentId(submissionAssignmentId: String): Int
    fun save(peerReview: PeerReview): PeerReview
    fun saveAll(reviews: List<PeerReview>): List<PeerReview>
}
