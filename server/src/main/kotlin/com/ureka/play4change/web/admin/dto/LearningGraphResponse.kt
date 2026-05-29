package com.ureka.play4change.web.admin.dto

data class LearningGraphEdgeResponse(
    val topicId: String,
    val prerequisiteTopicId: String
)

data class LearningGraphResponse(
    val edges: List<LearningGraphEdgeResponse>
)
