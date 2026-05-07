package com.ureka.play4change.features.profile.data.http

import com.ureka.play4change.core.model.Badge
import com.ureka.play4change.core.model.BadgeIconType
import com.ureka.play4change.core.network.TokenStorage
import com.ureka.play4change.features.profile.domain.model.ProfileData
import com.ureka.play4change.features.profile.domain.repository.ProfileRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
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
    val level: Int,
    val currentDay: Int,
    val totalDays: Int
)

@Serializable
private data class UserBadgeDto(
    val microCompetenceName: String,
    val description: String,
    val topicTitle: String,
    val earnedAt: String   // ISO-8601 string, e.g. "2025-01-15T10:30:00Z"
)

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
        val profileResponse = client.get("/profile")
        val badgesResponse = client.get("/profile/badges")

        val dto = json.decodeFromString<UserProfileDto>(profileResponse.bodyAsText())
        val badgeDtos = json.decodeFromString<List<UserBadgeDto>>(badgesResponse.bodyAsText())

        return ProfileData(
            userId = dto.userId,
            name = dto.name,
            email = dto.email,
            streakDays = dto.streakDays,
            totalPoints = dto.totalPoints,
            accuracy = dto.accuracy,
            level = dto.level,
            currentDay = dto.currentDay,
            totalDays = dto.totalDays,
            badges = badgeDtos.map { it.toBadge() }
        )
    }

    override suspend fun signOut() {
        val refreshToken = tokenStorage.getRefreshToken()
        tokenStorage.clear()
        if (refreshToken != null) {
            runCatching {
                client.post("/auth/logout") {
                    contentType(ContentType.Application.Json)
                    setBody(LogoutRequestDto(refreshToken))
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun UserBadgeDto.toBadge(): Badge = Badge(
        id = microCompetenceName,
        titleKey = microCompetenceName,
        descriptionKey = description,
        iconType = topicTitle.toIconType(),
        isUnlocked = true,
        unlockedAt = runCatching { Instant.parse(earnedAt).toEpochMilliseconds() }.getOrNull()
    )

    private fun String.toIconType(): BadgeIconType = when (uppercase()) {
        "SUSTAINABILITY" -> BadgeIconType.COMPASS
        "DIGITAL" -> BadgeIconType.STAR
        "HEALTH" -> BadgeIconType.FLAME
        "ECONOMY" -> BadgeIconType.CALENDAR
        "CULTURE" -> BadgeIconType.FIRST_STEP
        else -> BadgeIconType.COMPASS
    }
}
