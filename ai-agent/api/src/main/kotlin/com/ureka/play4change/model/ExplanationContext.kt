package com.ureka.play4change.model

import com.ureka.play4change.domain.AudienceLevel

data class ExplanationContext(
    val userId: String,
    val taskId: String,
    val topicId: String,
    val taskDescription: String,
    val moduleObjective: String,
    val subjectDomain: String,
    val audienceLevel: AudienceLevel,
    val language: String,
    val errorPattern: ErrorPattern,
    /** Summaries of all struggle sessions the learner went through, oldest first. */
    val struggleHistory: List<StruggleSummary>
)

data class StruggleSummary(
    /** 1-based depth index of this struggle session. */
    val depth: Int,
    val errorPattern: ErrorPattern,
    /** Titles of the adaptive tasks attempted in this session. */
    val taskTitles: List<String>
)

data class ConversationMessage(val role: String, val content: String)
