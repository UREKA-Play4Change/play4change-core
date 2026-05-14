package com.ureka.play4change.features.explore.data.http

import com.ureka.play4change.features.explore.domain.model.Topic
import com.ureka.play4change.features.explore.domain.model.TopicIconType
import com.ureka.play4change.features.explore.domain.repository.ExploreRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// Network DTOs
// ---------------------------------------------------------------------------

@Serializable
private data class UserTopicDto(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val taskCount: Int,
    val isEnrolled: Boolean
)

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

class HttpExploreRepository(
    private val client: HttpClient
) : ExploreRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getTopics(userId: String): List<Topic> {
        val response = client.get("/topics")
        if (!response.status.isSuccess()) return emptyList()
        val dtos = json.decodeFromString<List<UserTopicDto>>(response.bodyAsText())
        return dtos.map { it.toTopic() }
    }

    override suspend fun enrollTopic(userId: String, topicId: String): Boolean {
        val response = client.post("/topics/$topicId/enroll") {
            contentType(ContentType.Application.Json)
        }
        return response.status.isSuccess()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun UserTopicDto.toTopic() = Topic(
        id = id,
        title = title,
        description = description,
        iconType = category.toIconType(),
        isActive = isEnrolled,
        taskCount = taskCount
    )

    private fun String.toIconType(): TopicIconType = when (uppercase()) {
        "SUSTAINABILITY" -> TopicIconType.SUSTAINABILITY
        "DIGITAL"        -> TopicIconType.DIGITAL
        "HEALTH"         -> TopicIconType.HEALTH
        "ECONOMY"        -> TopicIconType.ECONOMY
        "CULTURE"        -> TopicIconType.CULTURE
        else             -> TopicIconType.SUSTAINABILITY
    }
}
