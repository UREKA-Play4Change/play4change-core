package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "explanation_sessions")
class ExplanationSessionEntity(
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

    @Column(name = "explanation_text", columnDefinition = "TEXT")
    var explanationText: String? = null,

    @Column(nullable = false, length = 15)
    var status: String = "GENERATING",

    @Column(name = "generated_at", nullable = false)
    val generatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "resolved_at")
    var resolvedAt: OffsetDateTime? = null,

    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @OrderBy("sent_at ASC")
    val messages: MutableList<ExplanationMessageEntity> = mutableListOf()
)
