package com.ureka.play4change.features.peerreview.domain.model

data class VerdictResult(
    val verdict: String,
    val finalized: Boolean,
    val pointsAwarded: Int?
)
