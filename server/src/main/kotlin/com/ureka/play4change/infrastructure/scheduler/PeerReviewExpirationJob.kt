package com.ureka.play4change.infrastructure.scheduler

import com.ureka.play4change.domain.peerreview.PeerReviewRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class PeerReviewExpirationJob(
    private val peerReviewRepository: PeerReviewRepository
) {
    private val log = LoggerFactory.getLogger(PeerReviewExpirationJob::class.java)

    @Scheduled(fixedRateString = "\${scheduler.peer-review-expiration.rate-ms:1800000}")
    fun logExpiredReviews() {
        val expired = peerReviewRepository.findExpiredPending(OffsetDateTime.now())
        if (expired.isEmpty()) return
        // Expired slots are re-opened passively: selectAndAssignReview already skips expired pending
        // reviews when computing eligibility, so the next reviewer request picks up the submission.
        log.info("{} expired peer review slot(s) detected — slots re-opened for reassignment", expired.size)
    }
}
