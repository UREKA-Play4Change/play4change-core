package com.ureka.play4change.web.user.dto

import com.ureka.play4change.domain.peerreview.PeerReview

data class PeerReviewAssignmentResponse(
    val reviewId: String,
    val submissionPhotoUrl: String?
) {
    companion object {
        fun from(review: PeerReview, photoUrl: String?) = PeerReviewAssignmentResponse(
            reviewId = review.id,
            submissionPhotoUrl = photoUrl
        )
    }
}
