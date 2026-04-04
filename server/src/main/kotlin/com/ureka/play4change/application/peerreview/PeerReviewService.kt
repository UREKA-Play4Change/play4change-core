package com.ureka.play4change.application.peerreview

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.PeerReviewUseCase
import com.ureka.play4change.application.port.PendingReviewItem
import com.ureka.play4change.application.port.SubmitVerdictCommand
import com.ureka.play4change.application.port.VerdictResult
import com.ureka.play4change.application.port.VerdictSummary
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.peerreview.PeerReview
import com.ureka.play4change.domain.peerreview.PeerReviewRepository
import com.ureka.play4change.domain.peerreview.ReviewVerdict
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.Forbidden.ResourceOwnershipViolation
import com.ureka.play4change.error.client.NotFound
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class PeerReviewService(
    private val peerReviewRepository: PeerReviewRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val registry: MeterRegistry
) : PeerReviewUseCase {

    private val log = LoggerFactory.getLogger(PeerReviewService::class.java)

    companion object {
        const val REVIEW_POINTS_REWARD = 10
        private const val VERDICTS_REQUIRED = 3
    }

    override fun selectAndAssignReview(reviewerUserId: String, topicId: String): Either<AppError, PeerReview?> =
        either {
            val candidates = enrollmentRepository.findPendingReviewSubmissionsForTopic(topicId, reviewerUserId)

            val eligible = candidates.mapNotNull { submission ->
                val reviews = peerReviewRepository.findBySubmissionAssignmentId(submission.id)
                if (reviews.any { it.reviewerUserId == reviewerUserId }) return@mapNotNull null
                if (reviews.size >= VERDICTS_REQUIRED) return@mapNotNull null
                Pair(submission, reviews.size)
            }

            if (eligible.isEmpty()) return@either null

            val unreviewed = eligible.filter { (_, count) -> count == 0 }
            val selectedSubmission = if (unreviewed.isNotEmpty()) {
                unreviewed.random().first
            } else {
                eligible.random().first
            }

            peerReviewRepository.save(
                PeerReview(
                    id = UUID.randomUUID().toString(),
                    submissionAssignmentId = selectedSubmission.id,
                    reviewerUserId = reviewerUserId,
                    verdict = null,
                    comment = null,
                    assignedAt = OffsetDateTime.now(),
                    reviewedAt = null
                )
            )
        }

    override fun submitVerdict(command: SubmitVerdictCommand): Either<AppError, VerdictResult> = either {
        val review = ensureNotNull(peerReviewRepository.findById(command.reviewId)) {
            NotFound.ResourceNotFound("PeerReview", command.reviewId)
        }
        ensure(review.reviewerUserId == command.userId) {
            ResourceOwnershipViolation("PeerReview")
        }
        ensure(review.verdict == null) {
            Conflict.ConcurrentModification
        }

        val updatedReview = peerReviewRepository.save(
            review.copy(
                verdict = command.verdict,
                comment = command.comment,
                reviewedAt = OffsetDateTime.now()
            )
        )

        val allReviews = peerReviewRepository.findBySubmissionAssignmentId(review.submissionAssignmentId)
        val completed = allReviews.filter { it.verdict != null }

        val summary = VerdictSummary(
            correct = completed.count { it.verdict == ReviewVerdict.CORRECT },
            incorrect = completed.count { it.verdict == ReviewVerdict.INCORRECT },
            total = completed.size
        )

        val willFinalize = completed.size >= VERDICTS_REQUIRED
        registry.counter(
            "peer_reviews_completed_total",
            "finalized", willFinalize.toString()
        ).increment()

        if (!willFinalize) {
            return@either VerdictResult(
                peerReview = updatedReview,
                verdictSummary = summary,
                finalized = false,
                submitterPointsAwarded = null
            )
        }

        // Finalize: majority vote
        val submission = ensureNotNull(enrollmentRepository.findAssignmentById(review.submissionAssignmentId)) {
            NotFound.ResourceNotFound("TaskAssignment", review.submissionAssignmentId)
        }
        val submitterEnrollment = ensureNotNull(enrollmentRepository.findById(submission.enrollmentId)) {
            NotFound.ResourceNotFound("Enrollment", submission.enrollmentId)
        }
        val template = ensureNotNull(taskTemplateRepository.findById(submission.taskTemplateId)) {
            NotFound.ResourceNotFound("TaskTemplate", submission.taskTemplateId)
        }

        val isCorrect = summary.correct >= 2
        val pointsAwarded = if (isCorrect) template.pointsReward else 0
        val finalStatus = if (submission.submittedAt != null && submission.submittedAt.isAfter(submission.dueAt))
            AssignmentStatus.LATE else AssignmentStatus.SUBMITTED

        enrollmentRepository.saveAssignment(
            submission.copy(
                isCorrect = isCorrect,
                pointsAwarded = pointsAwarded,
                status = finalStatus
            )
        )

        enrollmentRepository.save(submitterEnrollment.addPoints(pointsAwarded))

        val topicId = submitterEnrollment.topicId
        completed.forEach { r ->
            val reviewerEnrollment = enrollmentRepository.findByUserIdAndTopicId(r.reviewerUserId, topicId)
            if (reviewerEnrollment != null) {
                enrollmentRepository.save(reviewerEnrollment.addPoints(REVIEW_POINTS_REWARD))
            }
        }

        log.info(
            "Submission {} finalized — correct={}, points={} awarded to submitter",
            submission.id, isCorrect, pointsAwarded
        )

        VerdictResult(
            peerReview = updatedReview,
            verdictSummary = summary,
            finalized = true,
            submitterPointsAwarded = pointsAwarded
        )
    }

    override fun getPendingReviews(userId: String, topicId: String): Either<AppError, List<PendingReviewItem>> =
        either {
            peerReviewRepository.findPendingByReviewerUserId(userId)
                .mapNotNull { review ->
                    val submission = enrollmentRepository.findAssignmentById(review.submissionAssignmentId)
                        ?: return@mapNotNull null
                    val enrollment = enrollmentRepository.findById(submission.enrollmentId)
                        ?: return@mapNotNull null
                    if (enrollment.topicId != topicId) return@mapNotNull null
                    PendingReviewItem(review = review, submissionPhotoUrl = submission.photoUrl)
                }
        }
}
