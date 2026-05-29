package com.ureka.play4change.features.explore.data.mock

import com.ureka.play4change.features.explore.domain.model.EnrollmentStatus
import com.ureka.play4change.features.explore.domain.model.Topic
import com.ureka.play4change.features.explore.domain.model.TopicIconType
import com.ureka.play4change.features.explore.domain.repository.ExploreRepository
import kotlinx.coroutines.delay

class MockExploreRepository : ExploreRepository {
    override suspend fun getTopics(userId: String): List<Topic> {
        delay(600)
        return listOf(
            Topic("sustainability", "Sustainability", "Learn about climate, recycling, and green living.",
                TopicIconType.SUSTAINABILITY, enrollmentStatus = EnrollmentStatus.ACTIVE, taskCount = 14),
            Topic("digital_literacy", "Digital Literacy", "Navigate the digital world safely and effectively.",
                TopicIconType.DIGITAL, enrollmentStatus = EnrollmentStatus.COMPLETED, taskCount = 10),
            Topic("health", "Health & Wellbeing", "Daily habits for a healthier, happier life.",
                TopicIconType.HEALTH, enrollmentStatus = null, taskCount = 12),
            Topic("economy", "Circular Economy", "Understand sustainable consumption and production.",
                TopicIconType.ECONOMY, enrollmentStatus = EnrollmentStatus.PAUSED, taskCount = 8),
            Topic("culture", "Culture & Society", "Explore how culture shapes our everyday choices.",
                TopicIconType.CULTURE, enrollmentStatus = null, isLocked = true, taskCount = 6),
        )
    }

    override suspend fun enrollTopic(userId: String, topicId: String): Boolean {
        delay(800)
        return true
    }

    override suspend fun deactivateEnrollment(userId: String, topicId: String): Boolean {
        delay(400)
        return true
    }
}
