package com.ureka.play4change.features.home.domain.model

import com.ureka.play4change.core.model.RoadmapNode
import com.ureka.play4change.design.components.DayStatus

data class HomeData(
    val userName: String,
    val streakDays: Int,
    val totalPoints: Int,
    val level: Int,
    val xpProgress: Float,
    val weekProgress: List<DayStatus>,
    val roadmapNodes: List<RoadmapNode>,
    val todayTask: TaskSummary?,
    val todayCompleted: Boolean
)

data class TaskSummary(
    val id: String,
    val title: String,
    val domain: String,
    val pointsReward: Int
)
