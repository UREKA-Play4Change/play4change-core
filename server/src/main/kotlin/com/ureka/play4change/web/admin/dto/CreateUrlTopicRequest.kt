package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.domain.topic.AudienceLevel
import java.time.OffsetDateTime

/**
 * Accepts the web frontend shape as the primary form:
 *   { title, description, category, urls: string[], durationDays: int, difficulty: string }
 *
 * All legacy fields are kept as nullable so existing demo/CLI clients continue to work.
 * Resolution helpers (`resolved*`) compute the canonical value used by the application layer.
 */
data class CreateUrlTopicRequest(
    val title: String,
    val description: String,
    val category: String = "",

    // Web frontend sends an array; legacy CLI sends a single string.
    val urls: List<String> = emptyList(),
    val url: String? = null,

    // Web frontend uses durationDays (3–7); legacy CLI uses subscriptionWindowDays.
    val durationDays: Int? = null,
    val subscriptionWindowDays: Int? = null,

    // Web frontend uses difficulty ("BEGINNER"|"INTERMEDIATE"|"ADVANCED"); legacy uses audienceLevel.
    val difficulty: String? = null,
    val audienceLevel: AudienceLevel? = null,

    val language: String = "en",

    // taskCount and expiresAt are computed when not supplied by the client.
    val taskCount: Int? = null,
    val expiresAt: OffsetDateTime? = null
) {
    fun resolvedUrl(): String = url ?: urls.firstOrNull() ?: ""

    fun resolvedDurationDays(): Int = subscriptionWindowDays ?: durationDays ?: 5

    fun resolvedAudienceLevel(): AudienceLevel =
        audienceLevel
            ?: difficulty?.let { runCatching { AudienceLevel.valueOf(it) }.getOrNull() }
            ?: AudienceLevel.BEGINNER

    /** 3 tasks per day; use explicit taskCount when provided. */
    fun resolvedTaskCount(): Int = taskCount ?: (resolvedDurationDays() * 3)

    fun resolvedExpiresAt(): OffsetDateTime =
        expiresAt ?: OffsetDateTime.now().plusDays(resolvedDurationDays().toLong() + 30)
}
