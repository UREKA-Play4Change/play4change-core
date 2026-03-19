package com.ureka.play4change.features.explore.domain.repository

import com.ureka.play4change.features.explore.domain.model.Topic

interface ExploreRepository {
    suspend fun getTopics(userId: String): List<Topic>
    suspend fun switchTopic(userId: String, newTopicId: String): Boolean
}
