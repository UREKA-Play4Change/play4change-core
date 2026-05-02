package com.ureka.play4change.web.user.dto

import com.ureka.play4change.application.port.UserBadgeDto
import java.time.OffsetDateTime

data class UserBadgeResponse(
    val microCompetenceName: String,
    val description: String,
    val topicTitle: String,
    val earnedAt: OffsetDateTime
) {
    companion object {
        fun from(dto: UserBadgeDto) = UserBadgeResponse(
            microCompetenceName = dto.microCompetenceName,
            description = dto.description,
            topicTitle = dto.topicTitle,
            earnedAt = dto.earnedAt
        )
    }
}
