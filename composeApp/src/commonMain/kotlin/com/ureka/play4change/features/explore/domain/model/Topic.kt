package com.ureka.play4change.features.explore.domain.model

data class Topic(
    val id: String,
    val title: String,
    val description: String,
    val iconType: TopicIconType,
    val isActive: Boolean,
    val taskCount: Int,
    val isLocked: Boolean = false,
    val prerequisiteTopicIds: List<String> = emptyList()
)

enum class TopicIconType { SUSTAINABILITY, DIGITAL, HEALTH, ECONOMY, CULTURE }
