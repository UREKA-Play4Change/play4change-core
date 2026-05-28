package com.ureka.play4change.domain.topic

interface TopicStatsRepository {
    /** Returns stats for one topic. Always returns a value (zeros when no enrollments yet). */
    fun getForTopic(topicId: String): TopicStats

    /** Batch variant — one query for many topics. Missing entries mean no enrollments. */
    fun getForTopics(topicIds: List<String>): Map<String, TopicStats>
}
