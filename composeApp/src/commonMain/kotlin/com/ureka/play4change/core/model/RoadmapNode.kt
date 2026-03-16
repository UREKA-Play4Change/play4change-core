package com.ureka.play4change.core.model

data class RoadmapNode(
    val dayIndex: Int,
    val title: String,
    val status: NodeStatus,
    val isAdaptiveBranch: Boolean = false,
    val pointsReward: Int = 0
)

enum class NodeStatus {
    Completed,
    Current,
    Available,
    Locked
}
