package com.ureka.play4change.features.explore.domain.model

data class Topic(
    val id: String,
    val title: String,
    val description: String,
    val iconType: TopicIconType,
    val enrollmentStatus: EnrollmentStatus? = null,
    val taskCount: Int,
    val isLocked: Boolean = false,
    val prerequisiteTopicIds: List<String> = emptyList()
) {
    val isActive: Boolean get() = enrollmentStatus == EnrollmentStatus.ACTIVE
    val isCompleted: Boolean get() = enrollmentStatus == EnrollmentStatus.COMPLETED
    val isAbandoned: Boolean get() = enrollmentStatus == EnrollmentStatus.PAUSED
}

enum class EnrollmentStatus { ACTIVE, COMPLETED, PAUSED }

enum class TopicIconType { SUSTAINABILITY, DIGITAL, HEALTH, ECONOMY, CULTURE }
