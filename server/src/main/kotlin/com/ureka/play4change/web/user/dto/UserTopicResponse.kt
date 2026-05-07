package com.ureka.play4change.web.user.dto

import com.ureka.play4change.domain.topic.Topic

data class UserTopicResponse(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val taskCount: Int,
    val isEnrolled: Boolean
) {
    companion object {
        fun from(topic: Topic, isEnrolled: Boolean) = UserTopicResponse(
            id = topic.id,
            title = topic.title,
            description = topic.description,
            category = topic.category,
            taskCount = topic.taskCount,
            isEnrolled = isEnrolled
        )
    }
}
