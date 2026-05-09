package com.ureka.play4change.features.home.data.http

import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.TokenStorage
import com.ureka.play4change.core.network.networkErrorFromStatus
import com.ureka.play4change.features.home.domain.model.HomeData
import com.ureka.play4change.features.home.domain.model.TaskSummary
import com.ureka.play4change.features.home.domain.repository.HomeRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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
private data class TodayTaskDto(
    val assignmentId: String,
    val title: String,
    val pointsReward: Int
)

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

/**
 * HTTP-backed [HomeRepository].
 *
 * Composes two server calls:
 * - `GET /profile` — supplies user name, streak, points, level, and XP progress.
 * - `GET /tasks/today` — supplies today's task summary (optional; null if the
 *   user has no task today or has already completed it).
 *
 * UI-only fields ([HomeData.weekProgress] and [HomeData.roadmapNodes]) are not
 * yet served by a dedicated endpoint and default to empty lists. They will be
 * populated in a future phase when the corresponding API endpoints are added.
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

        var todayTask: TaskSummary? = null
        var todayCompleted = false
        try {
            val taskResponse = client.get("/tasks/today")
            when (taskResponse.status) {
                HttpStatusCode.OK -> {
                    val dto = json.decodeFromString<TodayTaskDto>(taskResponse.bodyAsText())
                    todayTask = TaskSummary(
                        id = dto.assignmentId,
                        title = dto.title,
                        domain = "",
                        pointsReward = dto.pointsReward
                    )
                }
                HttpStatusCode.NotFound -> todayCompleted = true
                else -> { /* keep todayTask = null */ }
            }
        } catch (_: Exception) {
            // Task fetch is best-effort; a failed call does not block the home screen.
        }

        return HomeData(
            userName = profile.name,
            streakDays = profile.streakDays,
            totalPoints = profile.totalPoints,
            level = profile.level,
            xpProgress = profile.currentDay.toFloat() / maxOf(profile.totalDays, 1),
            weekProgress = emptyList(),
            roadmapNodes = emptyList(),
            todayTask = todayTask,
            todayCompleted = todayCompleted
        )
    }
}
