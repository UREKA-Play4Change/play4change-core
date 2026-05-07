package com.ureka.play4change.features.peerreview.domain.repository

import com.ureka.play4change.features.peerreview.domain.model.PendingReview
import com.ureka.play4change.features.peerreview.domain.model.VerdictResult

interface PeerReviewRepository {
    suspend fun getPendingReviews(topicId: String): List<PendingReview>
    suspend fun submitVerdict(reviewId: String, verdict: String, comment: String?): VerdictResult
}
