package com.ureka.play4change.domain.topic

interface PrerequisiteRepository {
    /** Returns the IDs of topics that must be completed before [topicId] can be enrolled in. */
    fun findPrerequisitesByTopicId(topicId: String): List<String>

    /** Replaces all prerequisites for [topicId] with [prerequisiteIds] atomically. */
    fun setPrerequisites(topicId: String, prerequisiteIds: List<String>)

    /** Returns every directed edge (topicId → prerequisiteTopicId) in the graph. */
    fun findAllEdges(): List<Pair<String, String>>
}
