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
    name = "task_reports",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "task_template_id"])]
)
class TaskReportEntity(

    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_template_id", nullable = false)
    val taskTemplate: TaskTemplateEntity,

    @Column(name = "user_id", nullable = false, length = 36)
    val userId: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val reason: String,

    @Column(nullable = false, length = 20)
    var status: String = "PENDING",

    @Column(name = "reported_at", nullable = false)
    val reportedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "resolved_at")
    var resolvedAt: OffsetDateTime? = null
)
