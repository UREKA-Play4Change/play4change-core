package com.ureka.play4change.domain.badge

interface BadgeRepository {
    fun findMicroCompetenceByTopicId(topicId: String): MicroCompetence?
    fun findMicroCompetenceById(id: String): MicroCompetence?
    fun findBadgeByUserIdAndMicroCompetenceId(userId: String, microCompetenceId: String): Badge?
    fun findBadgesByUserId(userId: String): List<Badge>
    fun findBadgesByMicroCompetenceId(microCompetenceId: String): List<Badge>
    fun saveBadge(badge: Badge): Badge
    fun saveMicroCompetence(microCompetence: MicroCompetence): MicroCompetence
}
