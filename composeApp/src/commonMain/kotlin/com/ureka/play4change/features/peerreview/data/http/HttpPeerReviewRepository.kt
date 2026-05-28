package com.ureka.play4change.features.peerreview.data.http

import com.ureka.play4change.features.peerreview.domain.model.PendingReview
import com.ureka.play4change.features.peerreview.domain.model.VerdictResult
import com.ureka.play4change.features.peerreview.domain.repository.PeerReviewRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// Network DTOs
// ---------------------------------------------------------------------------

@Serializable
private data class PendingReviewDto(
    val reviewId: String,
    val submissionPhotoUrl: String? = null
)

@Serializable
private data class SubmitVerdictRequestDto(
    val verdict: String,
    val comment: String? = null
)

@Serializable
private data class VerdictResultDto(
    val verdict: String,
    val finalized: Boolean,
    val submitterPointsAwarded: Int? = null
)

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

class HttpPeerReviewRepository(
    private val client: HttpClient
) : PeerReviewRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getPendingReviews(topicId: String): List<PendingReview> {
        val response = client.get("reviews/pending") {
            parameter("topicId", topicId)
        }
        return json.decodeFromString<List<PendingReviewDto>>(response.bodyAsText())
            .map { PendingReview(reviewId = it.reviewId, photoUrl = it.submissionPhotoUrl) }
    }

    override suspend fun submitVerdict(
        reviewId: String,
        verdict: String,
        comment: String?
    ): VerdictResult {
        val response = client.post("reviews/$reviewId/verdict") {
            contentType(ContentType.Application.Json)
            setBody(SubmitVerdictRequestDto(verdict = verdict, comment = comment))
        }
        val dto = json.decodeFromString<VerdictResultDto>(response.bodyAsText())
        return VerdictResult(
            verdict = dto.verdict,
            finalized = dto.finalized,
            pointsAwarded = dto.submitterPointsAwarded
        )
    }
}
