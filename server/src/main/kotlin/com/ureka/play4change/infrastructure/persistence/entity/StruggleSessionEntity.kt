package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "struggle_sessions")
class StruggleSessionEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    val enrollment: EnrollmentEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_task_assignment_id", nullable = false)
    val originalTaskAssignment: TaskAssignmentEntity,

    @Column(name = "error_pattern", nullable = false, length = 30)
    val errorPattern: String,

    @Column(name = "attempt_count", nullable = false)
    val attemptCount: Int = 2,

    @Column(name = "detected_at", nullable = false)
    val detectedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "resolved_at")
    var resolvedAt: OffsetDateTime? = null,

    @Column(nullable = false, length = 15)
    var status: String = "OPEN",

    @OneToMany(mappedBy = "struggleSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @OrderBy("order_index ASC")
    val adaptiveTasks: MutableList<AdaptiveTaskEntity> = mutableListOf()
)
