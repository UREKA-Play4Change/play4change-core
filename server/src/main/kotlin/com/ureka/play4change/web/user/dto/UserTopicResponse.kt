package com.ureka.play4change.web.user.dto

import com.ureka.play4change.domain.topic.Topic

data class UserTopicResponse(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val taskCount: Int,
    val isEnrolled: Boolean,
    val enrollmentStatus: String?,
    val isLocked: Boolean,
    val prerequisiteTopicIds: List<String>
) {
    companion object {
        fun from(
            topic: Topic,
            enrollmentStatus: String?,
            isLocked: Boolean,
            prerequisiteTopicIds: List<String>
        ) = UserTopicResponse(
            id = topic.id,
            title = topic.title,
            description = topic.description,
            category = topic.category,
            taskCount = topic.taskCount,
            isEnrolled = enrollmentStatus == "ACTIVE",
            enrollmentStatus = enrollmentStatus,
            isLocked = isLocked,
            prerequisiteTopicIds = prerequisiteTopicIds
        )
    }
}
