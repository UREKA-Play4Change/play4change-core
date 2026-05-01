package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "user_badges",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "micro_competence_id"])]
)
class BadgeEntity(

    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false, length = 36)
    val userId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "micro_competence_id", nullable = false)
    val microCompetence: MicroCompetenceEntity,

    @Column(name = "earned_at", nullable = false)
    val earnedAt: OffsetDateTime = OffsetDateTime.now()
)
