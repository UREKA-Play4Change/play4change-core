package com.ureka.play4change.features.explore.domain.model

data class Topic(
    val id: String,
    val title: String,
    val description: String,
    val iconType: TopicIconType,
    val isActive: Boolean,
    val taskCount: Int
)

enum class TopicIconType { SUSTAINABILITY, DIGITAL, HEALTH, ECONOMY, CULTURE }
