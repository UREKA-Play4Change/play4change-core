package com.ureka.play4change.domain

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class TaskType { MULTIPLE_CHOICE, TRUE_FALSE, TODO_ACTION }
enum class TaskStatus { PENDING, COMPLETED, LATE }

// ── Internal domain objects (never sent to client) ────────────────────────────

data class TaskTemplate(
    val id: String,
    val moduleId: String,
    val dayIndex: Int,
    val title: String,
    val description: String,
    val hint: String?,
    val taskType: TaskType,
    val pointsReward: Int,
    val options: List<String>?,
    val correctAnswer: Int?,          // never sent to client
    val requiresAiValidation: Boolean
)

data class UserTask(
    val id: String,
    val userId: String,
    val taskTemplateId: String,
    val status: TaskStatus,
    val selectedOption: Int?,
    val textAnswer: String?,
    val isCorrect: Boolean?,
    val pointsAwarded: Int,
    val optionOrder: List<Int>
)

// ── Request / Response DTOs ───────────────────────────────────────────────────

data class TaskResponse(
    val userTaskId: String,
    val title: String,
    val description: String,
    val hint: String?,
    val taskType: TaskType,
    val pointsReward: Int,
    val options: List<String>?        // shuffled — correctAnswer never included
)

data class SubmitRequest(
    val selectedOption: Int?,         // MULTIPLE_CHOICE / TRUE_FALSE
    val textAnswer: String?           // TODO_ACTION
)

data class SubmitResponse(
    val userTaskId: String,
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val status: TaskStatus,
    val feedback: String?,
    val isLate: Boolean
)

data class CreateCourseRequest(
    val title: String,
    val subjectDomain: String,
    val moduleTitle: String,
    val moduleObjective: String,
    val durationDays: Int
)

data class CreateCourseResponse(
    val courseId: String,
    val moduleId: String,
    val title: String,
    val durationDays: Int,
    val tasksSeeded: Int
)

data class EnrollResponse(
    val subscriptionId: String,
    val enrolledAt: java.time.OffsetDateTime
)