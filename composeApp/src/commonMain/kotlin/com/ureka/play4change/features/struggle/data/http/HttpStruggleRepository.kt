package com.ureka.play4change.features.struggle.data.http

import com.ureka.play4change.features.struggle.domain.model.AdaptiveSubmitResult
import com.ureka.play4change.features.struggle.domain.model.AdaptiveTask
import com.ureka.play4change.features.struggle.domain.model.StruggleSession
import com.ureka.play4change.features.struggle.domain.repository.StruggleRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// Network DTOs
// ---------------------------------------------------------------------------

@Serializable
private data class AdaptiveTaskDto(
    val taskId: String,
    val title: String,
    val description: String,
    val hint: String? = null,
    val options: List<String> = emptyList(),
    val pointsReward: Int
)

@Serializable
private data class StruggleSessionDto(
    val sessionId: String,
    val errorPattern: String,
    val status: String,
    val adaptiveTasks: List<AdaptiveTaskDto>
)

@Serializable
private data class SubmitAdaptiveTaskRequestDto(val selectedOption: Int)

@Serializable
private data class AdaptiveSubmitResultDto(
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val sessionResolved: Boolean
)

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

class HttpStruggleRepository(
    private val client: HttpClient
) : StruggleRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getSession(enrollmentId: String): StruggleSession? {
        val response = client.get("/struggle/enrollment/$enrollmentId")
        if (response.status == HttpStatusCode.NotFound) return null
        val dto = json.decodeFromString<StruggleSessionDto>(response.bodyAsText())
        return dto.toSession()
    }

    override suspend fun submitTask(
        sessionId: String,
        taskId: String,
        selectedOption: Int
    ): AdaptiveSubmitResult {
        val response = client.post("/struggle/$sessionId/tasks/$taskId/submit") {
            contentType(ContentType.Application.Json)
            setBody(SubmitAdaptiveTaskRequestDto(selectedOption))
        }
        val dto = json.decodeFromString<AdaptiveSubmitResultDto>(response.bodyAsText())
        return AdaptiveSubmitResult(
            isCorrect = dto.isCorrect,
            pointsAwarded = dto.pointsAwarded,
            sessionResolved = dto.sessionResolved
        )
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun StruggleSessionDto.toSession() = StruggleSession(
        sessionId = sessionId,
        errorPattern = errorPattern,
        status = status,
        tasks = adaptiveTasks.map { it.toTask() }
    )

    private fun AdaptiveTaskDto.toTask() = AdaptiveTask(
        taskId = taskId,
        title = title,
        description = description,
        hint = hint ?: "",
        options = options,
        pointsReward = pointsReward
    )
}
