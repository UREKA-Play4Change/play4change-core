package com.ureka.play4change.features.explore.data.mock

import com.ureka.play4change.features.explore.domain.model.Topic
import com.ureka.play4change.features.explore.domain.model.TopicIconType
import com.ureka.play4change.features.explore.domain.repository.ExploreRepository
import kotlinx.coroutines.delay

class MockExploreRepository : ExploreRepository {
    override suspend fun getTopics(userId: String): List<Topic> {
        delay(600)
        return listOf(
            Topic("sustainability", "Sustainability", "Learn about climate, recycling, and green living.",
                TopicIconType.SUSTAINABILITY, isActive = true, taskCount = 14),
            Topic("digital_literacy", "Digital Literacy", "Navigate the digital world safely and effectively.",
                TopicIconType.DIGITAL, isActive = false, taskCount = 10),
            Topic("health", "Health & Wellbeing", "Daily habits for a healthier, happier life.",
                TopicIconType.HEALTH, isActive = false, taskCount = 12),
            Topic("economy", "Circular Economy", "Understand sustainable consumption and production.",
                TopicIconType.ECONOMY, isActive = false, taskCount = 8),
        )
    }

    override suspend fun switchTopic(userId: String, newTopicId: String): Boolean {
        delay(800)
        // TODO: call real API
        return true
    }
}
