package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.badge.Badge
import com.ureka.play4change.domain.badge.BadgeRepository
import com.ureka.play4change.domain.badge.MicroCompetence
import com.ureka.play4change.domain.topic.PageResult
import com.ureka.play4change.infrastructure.persistence.entity.BadgeEntity
import com.ureka.play4change.infrastructure.persistence.entity.MicroCompetenceEntity
import com.ureka.play4change.infrastructure.persistence.repository.BadgeJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.MicroCompetenceJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class BadgeRepositoryAdapter(
    private val microCompetenceJpa: MicroCompetenceJpaRepository,
    private val badgeJpa: BadgeJpaRepository
) : BadgeRepository {

    override fun findMicroCompetenceByTopicId(topicId: String): MicroCompetence? =
        microCompetenceJpa.findByTopicId(topicId)?.toDomain()

    override fun findMicroCompetenceById(id: String): MicroCompetence? =
        microCompetenceJpa.findById(id).orElse(null)?.toDomain()

    override fun findBadgeByUserIdAndMicroCompetenceId(userId: String, microCompetenceId: String): Badge? =
        badgeJpa.findByUserIdAndMicroCompetenceId(userId, microCompetenceId)?.toDomain()

    override fun saveBadge(badge: Badge): Badge {
        val competenceRef = microCompetenceJpa.getReferenceById(badge.microCompetenceId)
        return badgeJpa.save(
            BadgeEntity(
                id = badge.id,
                userId = badge.userId,
                microCompetence = competenceRef,
                earnedAt = badge.earnedAt
            )
        ).toDomain()
    }

    override fun findBadgesByUserId(userId: String): List<Badge> =
        badgeJpa.findByUserId(userId).map { it.toDomain() }

    override fun findBadgesByUserIdPaged(userId: String, page: Int, size: Int): PageResult<Badge> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "earnedAt"))
        val jpaPage = badgeJpa.findByUserId(userId, pageable)
        return PageResult(
            content = jpaPage.content.map { it.toDomain() },
            page = jpaPage.number,
            size = jpaPage.size,
            totalElements = jpaPage.totalElements,
            totalPages = jpaPage.totalPages
        )
    }

    override fun findBadgesByMicroCompetenceId(microCompetenceId: String): List<Badge> =
        badgeJpa.findByMicroCompetenceId(microCompetenceId).map { it.toDomain() }

    override fun saveMicroCompetence(microCompetence: MicroCompetence): MicroCompetence =
        microCompetenceJpa.save(
            MicroCompetenceEntity(
                id = microCompetence.id,
                name = microCompetence.name,
                description = microCompetence.description,
                topicId = microCompetence.topicId,
                iconUrl = microCompetence.iconUrl
            )
        ).toDomain()

    private fun MicroCompetenceEntity.toDomain() = MicroCompetence(
        id = id,
        name = name,
        description = description,
        topicId = topicId,
        iconUrl = iconUrl
    )

    private fun BadgeEntity.toDomain() = Badge(
        id = id,
        userId = userId,
        microCompetenceId = microCompetence.id,
        earnedAt = earnedAt
    )
}
