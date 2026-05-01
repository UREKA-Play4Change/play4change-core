package com.ureka.play4change.domain.topic

/**
 * A variant of a [TaskTemplate] with alternative distractors.
 * The correct answer index is identical to the parent template — only the wrong options differ.
 * One task template has N instances (N = task-generation.instances-per-task).
 */
data class TaskInstance(
    val id: String,
    val taskTemplateId: String,
    val instanceIndex: Int,
    val options: List<String>,
    val correctAnswer: Int
)
