package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "explanation_messages")
class ExplanationMessageEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: ExplanationSessionEntity,

    @Column(nullable = false, length = 10)
    val role: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "sent_at", nullable = false)
    val sentAt: OffsetDateTime = OffsetDateTime.now()
)
