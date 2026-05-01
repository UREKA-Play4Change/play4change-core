package com.ureka.play4change.domain.badge

import java.time.OffsetDateTime

data class Badge(
    val id: String,
    val userId: String,
    val microCompetenceId: String,
    val earnedAt: OffsetDateTime
)
