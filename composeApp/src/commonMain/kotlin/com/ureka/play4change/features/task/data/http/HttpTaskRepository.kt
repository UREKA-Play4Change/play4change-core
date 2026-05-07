package com.ureka.play4change.features.task.data.http

import com.ureka.play4change.features.task.domain.model.SubmitResult
import com.ureka.play4change.features.task.domain.model.TaskDetail
import com.ureka.play4change.features.task.domain.repository.TaskRepository
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
private data class TaskResponseDto(
    val assignmentId: String,
    val title: String,
    val description: String,
    val hint: String? = null,
    val options: List<String> = emptyList(),
    val pointsReward: Int,
)

@Serializable
private data class SubmitAnswerRequestDto(val selectedOption: Int)

@Serializable
private data class SubmitResultDto(
    val isCorrect: Boolean,
    val pointsAwarded: Int,
)

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
        val response = client.get("/tasks/today") {
            parameter("topicId", userTaskId)
        }
        val dto = json.decodeFromString<TaskResponseDto>(response.bodyAsText())
        return TaskDetail(
            userTaskId = dto.assignmentId,
            title = dto.title,
            description = dto.description,
            hint = dto.hint ?: "",
            options = dto.options,
            correctIndex = 0,   // server never reveals correct index to client
            pointsReward = dto.pointsReward,
            domain = ""         // not provided by server task endpoint
        )
    }

    override suspend fun submitAnswer(userTaskId: String, selectedIndex: Int): SubmitResult {
        val response = client.post("/tasks/$userTaskId/submit") {
            contentType(ContentType.Application.Json)
            setBody(SubmitAnswerRequestDto(selectedOption = selectedIndex))
        }
        val dto = json.decodeFromString<SubmitResultDto>(response.bodyAsText())
        return SubmitResult(isCorrect = dto.isCorrect, pointsAwarded = dto.pointsAwarded)
    }
}
