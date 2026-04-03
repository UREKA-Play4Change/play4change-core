package com.ureka.play4change.web.user.dto

import com.ureka.play4change.application.port.SubmitTodoResult

data class TodoSubmitResponse(
    val assignmentId: String,
    val status: String,
    val photoUrl: String?,
    val assignedReview: PeerReviewAssignmentResponse?
) {
    companion object {
        fun from(result: SubmitTodoResult) = TodoSubmitResponse(
            assignmentId = result.assignment.id,
            status = result.assignment.status.name,
            photoUrl = result.assignment.photoUrl,
            assignedReview = result.assignedReview?.let {
                PeerReviewAssignmentResponse.from(it, result.assignedReviewPhotoUrl)
            }
        )
    }
}
