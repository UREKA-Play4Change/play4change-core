package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "micro_competences",
    uniqueConstraints = [UniqueConstraint(columnNames = ["topic_id"])]
)
class MicroCompetenceEntity(

    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "topic_id", nullable = false, length = 36)
    val topicId: String,

    @Column(name = "icon_url")
    val iconUrl: String? = null
)
