package com.ureka.play4change.features.task.data.http

import com.ureka.play4change.core.camera.readPhotoBytes
import com.ureka.play4change.core.network.NetworkError
import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.networkErrorFromStatus
import com.ureka.play4change.features.task.domain.model.SubmitResult
import com.ureka.play4change.features.task.domain.model.TaskDetail
import com.ureka.play4change.features.task.domain.repository.TaskRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// Network DTOs
// ---------------------------------------------------------------------------

@Serializable
private data class TaskResponseDto(
    val assignmentId: String,
    val enrollmentId: String = "",
    val title: String,
    val description: String,
    val hint: String? = null,
    val options: List<String> = emptyList(),
    val pointsReward: Int,
    val dueAt: String = "",
    val wrongAttemptCount: Int = 0,
)

@Serializable
private data class SubmitAnswerRequestDto(val selectedOption: Int)

@Serializable
private data class SubmitResultDto(
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val totalPoints: Int = 0,
    val streakDays: Int = 0,
    val struggleTriggered: Boolean = false,
)

@Serializable
private data class SubmitPhotoRequestDto(val photoUrl: String)

@Serializable
private data class MediaUploadResponseDto(val url: String)

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

/**
 * HTTP-backed [TaskRepository].
 *
 * [getTask] receives the **topic ID** (what the home screen passes to the task
 * screen) and calls `GET /tasks/today?topicId=…`.  The returned [TaskDetail]
 * stores the server's **assignment ID** in [TaskDetail.userTaskId] so that
 * [submitAnswer] can POST to `POST /tasks/{assignmentId}/submit`.
 */
class HttpTaskRepository(
    private val client: HttpClient
) : TaskRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getTask(userTaskId: String): TaskDetail {
        val response = client.get("tasks/today") {
            parameter("topicId", userTaskId)
            header("X-Timezone", TimeZone.currentSystemDefault().id)
        }
        when (response.status) {
            HttpStatusCode.Accepted ->
                throw NetworkException(NetworkError.TaskGenerationPending)
            HttpStatusCode.NotFound ->
                throw NetworkException(NetworkError.NoTaskAvailable)
            HttpStatusCode.OK -> { /* fall through to parse */ }
            else ->
                throw NetworkException(networkErrorFromStatus(response.status.value))
        }
        val dto = json.decodeFromString<TaskResponseDto>(response.bodyAsText())
        return TaskDetail(
            userTaskId = dto.assignmentId,
            enrollmentId = dto.enrollmentId,
            title = dto.title,
            description = dto.description,
            hint = dto.hint ?: "",
            options = dto.options,
            correctIndex = 0,   // server never reveals correct index to client
            pointsReward = dto.pointsReward,
            domain = "",        // not provided by server task endpoint
            dueAt = dto.dueAt,
            wrongAttemptCount = dto.wrongAttemptCount
        )
    }

    override suspend fun submitAnswer(userTaskId: String, selectedIndex: Int): SubmitResult {
        val response = client.post("tasks/$userTaskId/submit") {
            contentType(ContentType.Application.Json)
            setBody(SubmitAnswerRequestDto(selectedOption = selectedIndex))
            header("X-Timezone", TimeZone.currentSystemDefault().id)
        }
        val dto = json.decodeFromString<SubmitResultDto>(response.bodyAsText())
        return SubmitResult(
            isCorrect = dto.isCorrect,
            pointsAwarded = dto.pointsAwarded,
            totalPoints = dto.totalPoints,
            streakDays = dto.streakDays,
            struggleTriggered = dto.struggleTriggered
        )
    }

    override suspend fun uploadPhoto(localPath: String): String {
        val bytes = readPhotoBytes(localPath)
            ?: throw NetworkException(NetworkError.Unknown("Could not read photo file"))
        val response = client.post("media/photo") {
            setBody(MultiPartFormDataContent(formData {
                append("photo", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
                })
            }))
        }
        if (response.status.value !in 200..299) {
            throw NetworkException(networkErrorFromStatus(response.status.value))
        }
        return json.decodeFromString<MediaUploadResponseDto>(response.bodyAsText()).url
    }

    override suspend fun submitPhoto(taskId: String, photoUrl: String): SubmitResult {
        val response = client.post("tasks/$taskId/submit-photo") {
            contentType(ContentType.Application.Json)
            setBody(SubmitPhotoRequestDto(photoUrl = photoUrl))
            header("X-Timezone", TimeZone.currentSystemDefault().id)
        }
        if (response.status.value !in 200..299) {
            throw NetworkException(networkErrorFromStatus(response.status.value))
        }
        // Photo submissions are not graded — acknowledge as correct with no points.
        return SubmitResult(isCorrect = true, pointsAwarded = 0)
    }
}
