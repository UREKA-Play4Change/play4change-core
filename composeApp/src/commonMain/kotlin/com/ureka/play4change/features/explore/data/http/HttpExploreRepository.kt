package com.ureka.play4change.features.explore.data.http

import com.ureka.play4change.features.explore.domain.model.EnrollmentStatus
import com.ureka.play4change.features.explore.domain.model.Topic
import com.ureka.play4change.features.explore.domain.model.TopicIconType
import com.ureka.play4change.features.explore.domain.model.TopicPage
import com.ureka.play4change.features.explore.domain.repository.ExploreRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
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
    val isEnrolled: Boolean,
    val enrollmentStatus: String? = null,
    val isLocked: Boolean = false,
    val prerequisiteTopicIds: List<String> = emptyList()
)

@Serializable
private data class PagedTopicResponseDto(
    val content: List<UserTopicDto>,
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 1
)

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

class HttpExploreRepository(
    private val client: HttpClient
) : ExploreRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getTopics(userId: String, page: Int, size: Int): TopicPage {
        val response = client.get("topics") {
            parameter("page", page)
            parameter("size", size)
        }
        if (!response.status.isSuccess()) return TopicPage(emptyList(), 1)
        val paged = json.decodeFromString<PagedTopicResponseDto>(response.bodyAsText())
        return TopicPage(
            content = paged.content.map { it.toTopic() },
            totalPages = paged.totalPages
        )
    }

    override suspend fun enrollTopic(userId: String, topicId: String): Boolean {
        val response = client.post("topics/$topicId/enroll") {
            contentType(ContentType.Application.Json)
        }
        return response.status.isSuccess()
    }

    override suspend fun deactivateEnrollment(userId: String, topicId: String): Boolean {
        val response = client.put("topics/$topicId/enrollment/deactivate")
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
        enrollmentStatus = enrollmentStatus?.toEnrollmentStatus(),
        taskCount = taskCount,
        isLocked = isLocked,
        prerequisiteTopicIds = prerequisiteTopicIds
    )

    private fun String.toEnrollmentStatus(): EnrollmentStatus? = when (uppercase()) {
        "ACTIVE"    -> EnrollmentStatus.ACTIVE
        "COMPLETED" -> EnrollmentStatus.COMPLETED
        "PAUSED"    -> EnrollmentStatus.PAUSED
        else        -> null
    }

    private fun String.toIconType(): TopicIconType = when (uppercase()) {
        "SUSTAINABILITY" -> TopicIconType.SUSTAINABILITY
        "DIGITAL"        -> TopicIconType.DIGITAL
        "HEALTH"         -> TopicIconType.HEALTH
        "ECONOMY"        -> TopicIconType.ECONOMY
        "CULTURE"        -> TopicIconType.CULTURE
        else             -> TopicIconType.SUSTAINABILITY
    }
}
