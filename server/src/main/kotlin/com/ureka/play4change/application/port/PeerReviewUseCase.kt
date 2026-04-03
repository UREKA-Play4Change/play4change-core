package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.domain.peerreview.PeerReview
import com.ureka.play4change.domain.peerreview.ReviewVerdict
import com.ureka.play4change.error.AppError

data class SubmitVerdictCommand(
    val userId: String,
    val reviewId: String,
    val verdict: ReviewVerdict,
    val comment: String?
)

data class VerdictSummary(
    val correct: Int,
    val incorrect: Int,
    val total: Int
)

data class VerdictResult(
    val peerReview: PeerReview,
    val verdictSummary: VerdictSummary,
    val finalized: Boolean,
    val submitterPointsAwarded: Int?
)

data class PendingReviewItem(
    val review: PeerReview,
    val submissionPhotoUrl: String?
)

interface PeerReviewUseCase {
    fun selectAndAssignReview(reviewerUserId: String, topicId: String): Either<AppError, PeerReview?>
    fun submitVerdict(command: SubmitVerdictCommand): Either<AppError, VerdictResult>
    fun getPendingReviews(userId: String, topicId: String): Either<AppError, List<PendingReviewItem>>
}
