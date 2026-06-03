package com.ureka.play4change.features.profile.data.http

import com.ureka.play4change.core.model.Badge
import com.ureka.play4change.core.model.BadgeIconType
import com.ureka.play4change.core.network.TokenStorage
import com.ureka.play4change.features.profile.domain.model.ProfileData
import com.ureka.play4change.features.profile.domain.repository.ProfileRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// Network DTOs
// ---------------------------------------------------------------------------

@Serializable
private data class UserProfileDto(
    val userId: String,
    val name: String,
    val email: String,
    val streakDays: Int,
    val totalPoints: Int,
    val accuracy: Float,
    val preferredLanguage: String = "en"
)

@Serializable
private data class UserBadgeDto(
    val microCompetenceName: String,
    val description: String,
    val topicTitle: String,
    val earnedAt: String
)

@Serializable
private data class TopicDto(
    val id: String,
    val title: String = "",
    val enrollmentStatus: String? = null
) {
    val hasEnrollment: Boolean get() = enrollmentStatus != null
}

@Serializable
private data class UpdateNameRequestDto(val name: String)

@Serializable
private data class UpdatePreferencesRequestDto(val language: String)

@Serializable
private data class LogoutRequestDto(val refreshToken: String)

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

class HttpProfileRepository(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage
) : ProfileRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getProfile(userId: String): ProfileData {
        val profileResponse = client.get("profile")
        val dto = json.decodeFromString<UserProfileDto>(profileResponse.bodyAsText())
        return dto.toProfileData(fetchTopicBadges())
    }

    override suspend fun updateName(name: String): ProfileData {
        val updateResponse = client.patch("profile") {
            contentType(ContentType.Application.Json)
            setBody(UpdateNameRequestDto(name))
        }
        val dto = json.decodeFromString<UserProfileDto>(updateResponse.bodyAsText())
        return dto.toProfileData(fetchTopicBadges())
    }

    override suspend fun updatePreferences(language: String) {
        client.put("profile/preferences") {
            contentType(ContentType.Application.Json)
            setBody(UpdatePreferencesRequestDto(language))
        }
    }

    override suspend fun signOut() {
        val refreshToken = tokenStorage.getRefreshToken()
        tokenStorage.clear()
        if (refreshToken != null) {
            runCatching {
                client.post("auth/logout") {
                    contentType(ContentType.Application.Json)
                    setBody(LogoutRequestDto(refreshToken))
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun UserProfileDto.toProfileData(badges: List<Badge>) = ProfileData(
        userId = userId,
        name = name,
        email = email,
        streakDays = streakDays,
        totalPoints = totalPoints,
        accuracy = accuracy,
        preferredLanguage = preferredLanguage,
        badges = badges
    )

    /**
     * Fetches the user's enrolled topics and earned badges, then produces one
     * [Badge] per enrolled topic: unlocked if the server has issued a badge for
     * that topic, locked otherwise.
     */
    private suspend fun fetchTopicBadges(): List<Badge> {
        val earned = runCatching {
            json.decodeFromString<List<UserBadgeDto>>(client.get("profile/badges").bodyAsText())
        }.getOrElse { emptyList() }

        val enrolledTopics = runCatching {
            json.decodeFromString<List<TopicDto>>(client.get("topics").bodyAsText())
                .filter { it.hasEnrollment }
        }.getOrElse { emptyList() }

        // Index earned badges by topic title (lowercase) for O(1) lookup.
        val earnedByTitle = earned.associateBy { it.topicTitle.trim().lowercase() }

        return enrolledTopics.map { topic ->
            val earnedBadge = earnedByTitle[topic.title.trim().lowercase()]
            val isCompleted = topic.enrollmentStatus?.uppercase() == "COMPLETED"
            Badge(
                id             = topic.id,
                titleKey       = topic.title,
                descriptionKey = topic.title,
                iconType       = BadgeIconType.STAR,
                isUnlocked     = earnedBadge != null || isCompleted,
                unlockedAt     = earnedBadge?.let {
                    runCatching { Instant.parse(it.earnedAt).toEpochMilliseconds() }.getOrNull()
                }
            )
        }
    }
}
