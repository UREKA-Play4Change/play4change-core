package com.ureka.play4change.repo

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID
import com.ureka.play4change.infra.converter.VectorConverter
import org.hibernate.annotations.ColumnTransformer

@Entity
@Table(name = "courses")
class CourseEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    @Column(nullable = false) val title: String,
    @Column(name = "subject_domain", nullable = false) val subjectDomain: String,
    @Column(nullable = false) val status: String = "IN_PROGRESS",
    @Column(name = "created_at", nullable = false) val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @OneToMany(mappedBy = "course", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val modules: MutableList<CourseModuleEntity> = mutableListOf()
)

@Entity
@Table(name = "course_modules")
class CourseModuleEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_id", nullable = false) val course: CourseEntity,
    @Column(name = "order_index", nullable = false) val orderIndex: Int = 0,
    @Column(nullable = false) val title: String,
    @Column(nullable = false) val topic: String,
    @Column(nullable = false) val objective: String,
    @OneToMany(mappedBy = "module", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val taskTemplates: MutableList<TaskTemplateEntity> = mutableListOf()
)

@Entity
@Table(name = "task_templates")
class TaskTemplateEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "module_id", nullable = false) val module: CourseModuleEntity,
    @Column(name = "day_index", nullable = false) val dayIndex: Int,
    @Column(name = "pool_index", nullable = false) val poolIndex: Int,
    @Column(nullable = false) val title: String,
    @Column(nullable = false, columnDefinition = "TEXT") val description: String,
    @Column(columnDefinition = "TEXT") val hint: String? = null,
    @Column(name = "task_type", nullable = false) val taskType: String = "MULTIPLE_CHOICE",
    @Column(name = "points_reward", nullable = false) val pointsReward: Int = 20,
    @Column(columnDefinition = "jsonb") @JdbcTypeCode(SqlTypes.JSON) val options: String? = null,
    @Column(name = "correct_answer") val correctAnswer: Int? = null,
    @Column(name = "requires_ai_validation", nullable = false) val requiresAiValidation: Boolean = false,
    @Column(name = "created_at", nullable = false) val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Convert(converter = VectorConverter::class)
    @Column(columnDefinition = "vector(384)")
    @ColumnTransformer(write = "?::vector")
    val embedding: FloatArray? = null
)

@Entity
@Table(name = "user_subscriptions")
class UserSubscriptionEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    @Column(name = "user_id", nullable = false) val userId: String,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_id", nullable = false) val course: CourseEntity,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "module_id", nullable = false) val module: CourseModuleEntity,
    @Column(name = "enrolled_at", nullable = false) val enrolledAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(nullable = false) val status: String = "ACTIVE"
)

@Entity
@Table(name = "user_tasks")
class UserTaskEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    @Column(name = "user_id", nullable = false) val userId: String,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "task_template_id", nullable = false) val taskTemplate: TaskTemplateEntity,
    @Column(name = "assigned_at", nullable = false) val assignedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "submitted_at") var submittedAt: OffsetDateTime? = null,
    @Column(nullable = false) var status: String = "PENDING",
    @Column(name = "selected_option") var selectedOption: Int? = null,
    @Column(name = "text_answer", columnDefinition = "TEXT") var textAnswer: String? = null,
    @Column(name = "is_correct") var isCorrect: Boolean? = null,
    @Column(name = "points_awarded", nullable = false) var pointsAwarded: Int = 0,
    @Column(name = "option_order", columnDefinition = "jsonb") @JdbcTypeCode(SqlTypes.JSON) var optionOrder: String? = null
)