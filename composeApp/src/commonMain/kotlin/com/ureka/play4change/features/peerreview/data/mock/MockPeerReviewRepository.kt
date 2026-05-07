package com.ureka.play4change.features.peerreview.data.mock

import com.ureka.play4change.features.peerreview.domain.model.PendingReview
import com.ureka.play4change.features.peerreview.domain.model.VerdictResult
import com.ureka.play4change.features.peerreview.domain.repository.PeerReviewRepository
import kotlinx.coroutines.delay

class MockPeerReviewRepository : PeerReviewRepository {

    override suspend fun getPendingReviews(topicId: String): List<PendingReview> {
        delay(600)
        return listOf(
            PendingReview(reviewId = "review-mock-001", photoUrl = null)
        )
    }

    override suspend fun submitVerdict(
        reviewId: String,
        verdict: String,
        comment: String?
    ): VerdictResult {
        delay(400)
        return VerdictResult(verdict = verdict, finalized = true, pointsAwarded = null)
    }
}
