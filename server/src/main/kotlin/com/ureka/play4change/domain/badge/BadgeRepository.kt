package com.ureka.play4change.domain.badge

interface BadgeRepository {
    fun findMicroCompetenceByTopicId(topicId: String): MicroCompetence?
    fun findBadgeByUserIdAndMicroCompetenceId(userId: String, microCompetenceId: String): Badge?
    fun saveBadge(badge: Badge): Badge
    fun findBadgesByUserId(userId: String): List<Badge>
    fun saveMicroCompetence(microCompetence: MicroCompetence): MicroCompetence
}
