package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "topic_prerequisites",
    uniqueConstraints = [UniqueConstraint(columnNames = ["topic_id", "prerequisite_topic_id"])]
)
class TopicPrerequisiteEntity(

    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "topic_id", nullable = false, length = 36)
    val topicId: String,

    @Column(name = "prerequisite_topic_id", nullable = false, length = 36)
    val prerequisiteTopicId: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
