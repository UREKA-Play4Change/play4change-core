package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.domain.topic.Topic

data class PrerequisiteTopicResponse(
    val id: String,
    val title: String,
    val status: String,
    val category: String
) {
    companion object {
        fun from(topic: Topic) = PrerequisiteTopicResponse(
            id = topic.id,
            title = topic.title,
            status = topic.status.name,
            category = topic.category
        )
    }
}
