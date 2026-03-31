package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "peer_reviews")
class PeerReviewEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_assignment_id", nullable = false)
    val submissionAssignment: TaskAssignmentEntity,

    @Column(name = "reviewer_user_id", nullable = false, length = 36)
    val reviewerUserId: String,

    @Column(length = 10)
    var verdict: String? = null,

    @Column(columnDefinition = "TEXT")
    var comment: String? = null,

    @Column(name = "assigned_at", nullable = false)
    val assignedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "reviewed_at")
    var reviewedAt: OffsetDateTime? = null
)
