package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "task_assignments")
class TaskAssignmentEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    val enrollment: EnrollmentEntity,

    @Column(name = "user_id", nullable = false, length = 36)
    val userId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_template_id", nullable = false)
    val taskTemplate: TaskTemplateEntity,

    @Column(name = "task_template_version", nullable = false)
    val taskTemplateVersion: Int = 1,

    @Column(name = "task_type", nullable = false, length = 20)
    val taskType: String,

    @Column(name = "assigned_at", nullable = false)
    val assignedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "due_at", nullable = false)
    val dueAt: OffsetDateTime,

    @Column(name = "submitted_at")
    var submittedAt: OffsetDateTime? = null,

    @Column(nullable = false, length = 10)
    var status: String = "PENDING",

    @Column(name = "selected_option")
    var selectedOption: Int? = null,

    @Column(name = "is_correct")
    var isCorrect: Boolean? = null,

    @Column(name = "points_awarded", nullable = false)
    var pointsAwarded: Int = 0,

    @Column(name = "option_order", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var optionOrder: String? = null,

    @Column(name = "wrong_attempt_count", nullable = false)
    var wrongAttemptCount: Int = 0,

    @Column(name = "photo_url", columnDefinition = "TEXT")
    var photoUrl: String? = null
)
