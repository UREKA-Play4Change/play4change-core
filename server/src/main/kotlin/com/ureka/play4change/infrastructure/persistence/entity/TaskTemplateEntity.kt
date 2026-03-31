package com.ureka.play4change.infrastructure.persistence.entity

import com.ureka.play4change.infra.converter.VectorConverter
import jakarta.persistence.*
import org.hibernate.annotations.ColumnTransformer
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "task_templates")
class TaskTemplateEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    val module: TopicModuleEntity,

    @Column(name = "day_index", nullable = false)
    val dayIndex: Int,

    @Column(name = "pool_index", nullable = false)
    val poolIndex: Int = 0,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(columnDefinition = "TEXT")
    val hint: String? = null,

    @Column(name = "task_type", nullable = false, length = 20)
    val taskType: String = "MULTIPLE_CHOICE",

    @Column(name = "points_reward", nullable = false)
    val pointsReward: Int = 20,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    val options: String? = null,

    @Column(name = "correct_answer")
    val correctAnswer: Int? = null,

    @Column(nullable = false)
    val version: Int = 1,

    @Column(name = "is_current", nullable = false)
    var isCurrent: Boolean = true,

    @Column(name = "superseded_by", length = 36)
    var supersededBy: String? = null,

    @Convert(converter = VectorConverter::class)
    @Column(columnDefinition = "vector(1024)")
    @ColumnTransformer(write = "?::vector")
    val embedding: FloatArray? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
