package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.topic.*
import com.ureka.play4change.infrastructure.persistence.entity.TopicEntity
import com.ureka.play4change.infrastructure.persistence.repository.TopicJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class TopicRepositoryAdapter(
    private val jpa: TopicJpaRepository
) : TopicRepository {

    override fun findById(id: String): Topic? =
        jpa.findById(id).orElse(null)?.toDomain()

    override fun findAllActive(): List<Topic> =
        jpa.findAllByStatus("ACTIVE").map { it.toDomain() }

    override fun findAll(): List<Topic> =
        jpa.findAll().map { it.toDomain() }

    override fun findAll(page: Int, size: Int): PageResult<Topic> {
        val pageResult = jpa.findAll(PageRequest.of(page, size))
        return PageResult(
            content = pageResult.content.map { it.toDomain() },
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    override fun findByStatus(status: TopicStatus): List<Topic> =
        jpa.findAllByStatus(status.name).map { it.toDomain() }

    override fun findByStatus(status: TopicStatus, page: Int, size: Int): PageResult<Topic> {
        val pageResult = jpa.findAllByStatus(status.name, PageRequest.of(page, size))
        return PageResult(
            content = pageResult.content.map { it.toDomain() },
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    override fun findActiveExpired(): List<Topic> =
        jpa.findAllByStatusAndExpiresAtBefore("ACTIVE", OffsetDateTime.now())
            .map { it.toDomain() }

    override fun findStuckGenerating(cutoff: OffsetDateTime): List<Topic> =
        jpa.findAllByStatusAndStatusUpdatedAtBefore("GENERATING", cutoff)
            .map { it.toDomain() }

    override fun save(topic: Topic): Topic =
        jpa.save(topic.toEntity()).toDomain()

    override fun updateStatus(id: String, status: TopicStatus) {
        jpa.findById(id).ifPresent { entity ->
            entity.status = status.name
            entity.statusUpdatedAt = OffsetDateTime.now()
            jpa.save(entity)
        }
    }

    private fun TopicEntity.toDomain(): Topic = Topic(
        id = id,
        title = title,
        description = description,
        category = category,
        contentSourceType = ContentSourceType.valueOf(contentSourceType),
        contentSourceRef = contentSourceRef,
        rawExtractedText = rawExtractedText,
        taskCount = taskCount,
        subscriptionWindowDays = subscriptionWindowDays,
        expiresAt = expiresAt,
        audienceLevel = AudienceLevel.valueOf(audienceLevel),
        language = language,
        status = TopicStatus.valueOf(status),
        createdBy = createdBy,
        createdAt = createdAt,
        version = version,
        statusUpdatedAt = statusUpdatedAt
    )

    private fun Topic.toEntity(): TopicEntity = TopicEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        contentSourceType = contentSourceType.name,
        contentSourceRef = contentSourceRef,
        rawExtractedText = rawExtractedText,
        taskCount = taskCount,
        subscriptionWindowDays = subscriptionWindowDays,
        expiresAt = expiresAt,
        audienceLevel = audienceLevel.name,
        language = language,
        status = status.name,
        createdBy = createdBy,
        createdAt = createdAt,
        statusUpdatedAt = statusUpdatedAt
    )
}
