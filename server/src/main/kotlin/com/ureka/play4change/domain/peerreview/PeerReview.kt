package com.ureka.play4change.domain.peerreview

import java.time.OffsetDateTime

data class PeerReview(
    val id: String,
    val submissionAssignmentId: String,
    val reviewerUserId: String,
    val verdict: ReviewVerdict?,
    val comment: String?,
    val assignedAt: OffsetDateTime,
    val reviewedAt: OffsetDateTime?
)
