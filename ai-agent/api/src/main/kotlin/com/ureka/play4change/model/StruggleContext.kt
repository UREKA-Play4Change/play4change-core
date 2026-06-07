package com.ureka.play4change.model

import com.ureka.play4change.domain.AudienceLevel

data class StruggleContext(
    val userId: String,
    val taskId: String,
    val moduleId: String,
    val topicId: String,
    val subjectDomain: String,
    val audienceLevel: AudienceLevel,
    val language: String,
    val attemptCount: Int,
    val errorPattern: ErrorPattern,
    val moduleObjective: String,     // keeps AI generation course-aware
    val taskDescription: String,     // the task the user is struggling with
    // Branch IDs this user has already seen for this assignment — excluded from similarity reuse
    // so follow-up struggle sessions never repeat questions the learner already failed.
    val excludedBranchIds: List<String> = emptyList()
)

enum class ErrorPattern {
    CONCEPTUAL_MISUNDERSTANDING,     // user doesn't understand the concept
    PROCEDURAL_ERROR,                // user understands but applies wrong steps
    KNOWLEDGE_GAP,                   // prerequisite knowledge missing
    UNCLEAR_INSTRUCTIONS,            // task description caused confusion
    TIME_PRESSURE,                   // user rushed; needs paced practice
    UNKNOWN                          // fallback — always generate fresh
}