package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.PeerReviewUseCase
import com.ureka.play4change.application.port.SubmitVerdictCommand
import com.ureka.play4change.domain.peerreview.ReviewVerdict
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.user.dto.PeerReviewAssignmentResponse
import com.ureka.play4change.web.user.dto.SubmitVerdictRequest
import com.ureka.play4change.web.user.dto.VerdictResultResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/reviews")
class PeerReviewController(private val peerReviewUseCase: PeerReviewUseCase) {

    @PostMapping("/{reviewId}/verdict")
    fun submitVerdict(
        @PathVariable reviewId: String,
        @RequestBody request: SubmitVerdictRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<VerdictResultResponse> {
        val verdict = runCatching { ReviewVerdict.valueOf(request.verdict.uppercase()) }
            .getOrNull()
            ?: return ResponseEntity.badRequest().build()

        return peerReviewUseCase.submitVerdict(
            SubmitVerdictCommand(
                userId = userId,
                reviewId = reviewId,
                verdict = verdict,
                comment = request.comment
            )
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(VerdictResultResponse.from(it)) }
        )
    }

    @GetMapping("/pending")
    fun getPendingReviews(
        @RequestParam topicId: String,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<List<PeerReviewAssignmentResponse>> =
        peerReviewUseCase.getPendingReviews(userId, topicId).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { items ->
                ResponseEntity.ok(
                    items.map { PeerReviewAssignmentResponse.from(it.review, it.submissionPhotoUrl) }
                )
            }
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
