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
        val badgesResponse = client.get("profile/badges")
        val dto = json.decodeFromString<UserProfileDto>(profileResponse.bodyAsText())
        val badgeDtos = json.decodeFromString<List<UserBadgeDto>>(badgesResponse.bodyAsText())
        return dto.toProfileData(badgeDtos.map { it.toBadge() })
    }

    override suspend fun updateName(name: String): ProfileData {
        val updateResponse = client.patch("profile") {
            contentType(ContentType.Application.Json)
            setBody(UpdateNameRequestDto(name))
        }
        val badgesResponse = client.get("profile/badges")
        val dto = json.decodeFromString<UserProfileDto>(updateResponse.bodyAsText())
        val badgeDtos = json.decodeFromString<List<UserBadgeDto>>(badgesResponse.bodyAsText())
        return dto.toProfileData(badgeDtos.map { it.toBadge() })
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

    private fun UserBadgeDto.toBadge(): Badge = Badge(
        id = microCompetenceName,
        titleKey = microCompetenceName,
        descriptionKey = description,
        iconType = microCompetenceName.toIconType(),
        isUnlocked = true,
        unlockedAt = runCatching { Instant.parse(earnedAt).toEpochMilliseconds() }.getOrNull()
    )

    private fun String.toIconType(): BadgeIconType = when (lowercase()) {
        "badge_first_task" -> BadgeIconType.FIRST_STEP
        "badge_streak_3", "badge_streak_7" -> BadgeIconType.FLAME
        "badge_perfect_quiz" -> BadgeIconType.STAR
        "badge_first_photo" -> BadgeIconType.CAMERA
        "badge_explorer" -> BadgeIconType.COMPASS
        else -> BadgeIconType.COMPASS
    }
}
