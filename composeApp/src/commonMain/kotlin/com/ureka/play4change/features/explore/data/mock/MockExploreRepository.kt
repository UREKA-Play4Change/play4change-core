package com.ureka.play4change.features.explore.data.mock

import com.ureka.play4change.features.explore.domain.model.EnrollmentStatus
import com.ureka.play4change.features.explore.domain.model.Topic
import com.ureka.play4change.features.explore.domain.model.TopicIconType
import com.ureka.play4change.features.explore.domain.model.TopicPage
import com.ureka.play4change.features.explore.domain.repository.ExploreRepository
import kotlinx.coroutines.delay

private val ALL_MOCK_TOPICS = listOf(
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
    Topic("ethics", "AI Ethics", "Understand the ethical implications of artificial intelligence.",
        TopicIconType.DIGITAL, enrollmentStatus = null, taskCount = 9),
    Topic("water", "Water Stewardship", "Learn responsible water use and conservation strategies.",
        TopicIconType.SUSTAINABILITY, enrollmentStatus = null, taskCount = 11),
)

class MockExploreRepository : ExploreRepository {
    override suspend fun getTopics(userId: String, page: Int, size: Int): TopicPage {
        delay(600)
        val start = page * size
        return TopicPage(
            content = ALL_MOCK_TOPICS.drop(start).take(size),
            totalPages = (ALL_MOCK_TOPICS.size + size - 1) / size
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
