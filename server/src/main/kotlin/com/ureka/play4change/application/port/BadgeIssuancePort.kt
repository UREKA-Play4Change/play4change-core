package com.ureka.play4change.application.port

interface BadgeIssuancePort {
    fun issueBadge(userId: String, topicId: String, enrollmentId: String)
}
