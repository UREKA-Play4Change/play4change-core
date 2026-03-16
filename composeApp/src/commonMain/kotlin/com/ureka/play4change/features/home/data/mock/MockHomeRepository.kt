package com.ureka.play4change.features.home.data.mock

import com.ureka.play4change.core.model.NodeStatus
import com.ureka.play4change.core.model.RoadmapNode
import com.ureka.play4change.design.components.DayStatus
import com.ureka.play4change.features.home.domain.model.HomeData
import com.ureka.play4change.features.home.domain.model.TaskSummary
import com.ureka.play4change.features.home.domain.repository.HomeRepository
import kotlinx.coroutines.delay

class MockHomeRepository : HomeRepository {
    override suspend fun getHomeData(userId: String): HomeData {
        delay(800)
        return HomeData(
            userName = "Radesh",
            streakDays = 7,
            totalPoints = 1340,
            level = 4,
            xpProgress = 0.62f,
            weekProgress = listOf(
                DayStatus.Completed, DayStatus.Completed, DayStatus.Completed,
                DayStatus.Completed, DayStatus.Completed, DayStatus.Today,
                DayStatus.Future
            ),
            roadmapNodes = listOf(
                RoadmapNode(0, "What is Sustainability?",  NodeStatus.Completed, pointsReward = 20),
                RoadmapNode(1, "Carbon Footprint Basics",  NodeStatus.Completed, pointsReward = 30),
                RoadmapNode(2, "Recycling at Home",        NodeStatus.Completed, pointsReward = 40),
                RoadmapNode(3, "Energy Consumption",       NodeStatus.Completed, pointsReward = 35),
                RoadmapNode(4, "Water Conservation",       NodeStatus.Completed, pointsReward = 30),
                RoadmapNode(4, "Water Basics (Help)",      NodeStatus.Completed, isAdaptiveBranch = true, pointsReward = 15),
                RoadmapNode(5, "Circular Economy",         NodeStatus.Current,   pointsReward = 50),
                RoadmapNode(6, "Digital Carbon Cost",      NodeStatus.Available, pointsReward = 45),
                RoadmapNode(7, "Sustainable Tech Choices", NodeStatus.Locked,    pointsReward = 60),
                RoadmapNode(8, "Final Challenge",          NodeStatus.Locked,    pointsReward = 100),
            ),
            todayTask = TaskSummary(
                id = "task-005",
                title = "Which product has the lowest lifecycle carbon footprint?",
                domain = "Sustainability",
                pointsReward = 50
            ),
            todayCompleted = false
        )
    }
}
