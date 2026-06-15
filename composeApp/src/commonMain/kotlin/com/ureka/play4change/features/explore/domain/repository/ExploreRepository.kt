package com.ureka.play4change.features.explore.domain.repository

import com.ureka.play4change.features.explore.domain.model.Topic
import com.ureka.play4change.features.explore.domain.model.TopicPage

interface ExploreRepository {
    suspend fun getTopics(userId: String, page: Int, size: Int): TopicPage
    suspend fun enrollTopic(userId: String, topicId: String): Boolean
    suspend fun deactivateEnrollment(userId: String, topicId: String): Boolean
}
