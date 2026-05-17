package com.ureka.play4change.features.home.data.http

import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.TokenStorage
import com.ureka.play4change.core.network.networkErrorFromStatus
import com.ureka.play4change.features.home.domain.model.HomeData
import com.ureka.play4change.features.home.domain.model.TaskSummary
import com.ureka.play4change.features.home.domain.model.PendingReviewSummary
import com.ureka.play4change.features.home.domain.model.TaskSummaryWithTopic
import com.ureka.play4change.features.home.domain.repository.HomeRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// Network DTOs
// ---------------------------------------------------------------------------

@Serializable
private data class UserProfileDto(
    val name: String,
    val streakDays: Int,
    val totalPoints: Int,
    val level: Int,
    val currentDay: Int,
    val totalDays: Int
)

@Serializable
private data class TopicSummaryDto(
    val id: String,
    val title: String = "",
    val isEnrolled: Boolean = false
)

@Serializable
private data class TodayTaskDto(
    val assignmentId: String,
    val title: String,
    val pointsReward: Int
)

@Serializable
private data class PendingReviewDto(
    val reviewId: String,
    val submissionPhotoUrl: String? = null
)

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

/**
 * HTTP-backed [HomeRepository].
 *
 * Composes two server calls:
 * - `GET /profile` — supplies user name, streak, points, level, and XP progress.
 * - `GET /topics` + `GET /tasks/today?topicId=...` (one call per enrolled topic,
 *   executed in parallel) — supplies each topic's daily task summary.
 *
 * UI-only fields ([HomeData.weekProgress] and [HomeData.roadmapNodes]) are not
 * yet served by a dedicated endpoint and default to empty lists.
 */
class HttpHomeRepository(
    private val client: HttpClient,
    @Suppress("UNUSED_PARAMETER") tokenStorage: TokenStorage
) : HomeRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getHomeData(userId: String): HomeData {
        val profileResponse = client.get("/profile")
        if (profileResponse.status.value !in 200..299) {
            throw NetworkException(networkErrorFromStatus(profileResponse.status.value))
        }
        val profile = json.decodeFromString<UserProfileDto>(profileResponse.bodyAsText())

        var todayTasks: List<TaskSummaryWithTopic> = emptyList()
        var pendingReviews: List<PendingReviewSummary> = emptyList()
        var isEnrolled = false
        try {
            val topicsResponse = client.get("/topics")
            val topics = json.decodeFromString<List<TopicSummaryDto>>(topicsResponse.bodyAsText())
            val enrolledTopics = topics.filter { it.isEnrolled }
            isEnrolled = enrolledTopics.isNotEmpty()

            coroutineScope {
                val tasksDeferred = enrolledTopics.map { topic ->
                    async { fetchTodayTask(topic) }
                }
                val reviewsDeferred = enrolledTopics.map { topic ->
                    async { fetchPendingReviews(topic) }
                }
                todayTasks = tasksDeferred.awaitAll()
                pendingReviews = reviewsDeferred.awaitAll().flatten()
            }
        } catch (_: Exception) {
            // Topic/task/review fetch is best-effort; a failed call does not block the home screen.
        }

        return HomeData(
            userName = profile.name,
            streakDays = profile.streakDays,
            totalPoints = profile.totalPoints,
            level = profile.level,
            xpProgress = profile.currentDay.toFloat() / maxOf(profile.totalDays, 1),
            weekProgress = emptyList(),
            roadmapNodes = emptyList(),
            todayTasks = todayTasks,
            pendingReviews = pendingReviews,
            isEnrolled = isEnrolled
        )
    }

    private suspend fun fetchTodayTask(topic: TopicSummaryDto): TaskSummaryWithTopic {
        return try {
            val taskResponse = client.get("/tasks/today") {
                parameter("topicId", topic.id)
            }
            when (taskResponse.status) {
                HttpStatusCode.OK -> {
                    val dto = json.decodeFromString<TodayTaskDto>(taskResponse.bodyAsText())
                    TaskSummaryWithTopic(
                        topicId = topic.id,
                        topicTitle = topic.title,
                        task = TaskSummary(
                            id = dto.assignmentId,
                            title = dto.title,
                            domain = "",
                            pointsReward = dto.pointsReward
                        ),
                        completed = false
                    )
                }
                // 202 Accepted: server is still generating the AI task content.
                HttpStatusCode.Accepted -> TaskSummaryWithTopic(
                    topicId = topic.id,
                    topicTitle = topic.title,
                    task = null,
                    completed = false,
                    isGenerating = true
                )
                // 404 with X-Task-Available-At: today's assignment is submitted (server confirms this
                // is an expected state, not a missing resource). Treat as completed.
                // 404 without the header: enrollment/topic not found or other error — don't falsely
                // show as completed; render nothing.
                HttpStatusCode.NotFound -> {
                    val hasAvailableAtHeader = taskResponse.headers["X-Task-Available-At"] != null
                    TaskSummaryWithTopic(
                        topicId = topic.id,
                        topicTitle = topic.title,
                        task = null,
                        completed = hasAvailableAtHeader
                    )
                }
                else -> TaskSummaryWithTopic(
                    topicId = topic.id,
                    topicTitle = topic.title,
                    task = null,
                    completed = false
                )
            }
        } catch (_: Exception) {
            TaskSummaryWithTopic(topicId = topic.id, topicTitle = topic.title, task = null, completed = false)
        }
    }

    private suspend fun fetchPendingReviews(topic: TopicSummaryDto): List<PendingReviewSummary> {
        return try {
            val response = client.get("/reviews/pending") {
                parameter("topicId", topic.id)
            }
            if (response.status.value !in 200..299) return emptyList()
            json.decodeFromString<List<PendingReviewDto>>(response.bodyAsText())
                .map { dto ->
                    PendingReviewSummary(
                        reviewId = dto.reviewId,
                        topicTitle = topic.title,
                        photoUrl = dto.submissionPhotoUrl
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
