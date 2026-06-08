package com.ureka.play4change.features.struggle.domain.model

data class AdaptiveSubmitResult(
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val sessionResolved: Boolean,
    val explanationSessionId: String? = null
)
