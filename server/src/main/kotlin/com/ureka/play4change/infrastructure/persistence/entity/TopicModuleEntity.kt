package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "topic_modules")
class TopicModuleEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    val topic: TopicEntity,

    @Column(name = "order_index", nullable = false)
    val orderIndex: Int = 0,

    @Column(nullable = false, columnDefinition = "TEXT")
    val objective: String
)
