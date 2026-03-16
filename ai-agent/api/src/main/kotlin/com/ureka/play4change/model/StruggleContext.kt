package com.ureka.play4change.model

import com.ureka.play4change.domain.AudienceLevel

data class StruggleContext(
    val userId: String,
    val taskId: String,
    val moduleId: String,
    val courseId: String,
    val subjectDomain: String,
    val audienceLevel: AudienceLevel,
    val language: String,
    val attemptCount: Int,
    val errorPattern: ErrorPattern,
    val moduleObjective: String,     // keeps AI generation course-aware
    val taskDescription: String      // the task the user is struggling with
)

enum class ErrorPattern {
    CONCEPTUAL_MISUNDERSTANDING,     // user doesn't understand the concept
    PROCEDURAL_ERROR,                // user understands but applies wrong steps
    KNOWLEDGE_GAP,                   // prerequisite knowledge missing
    UNCLEAR_INSTRUCTIONS,            // task description caused confusion
    UNKNOWN                          // fallback — always generate fresh
}