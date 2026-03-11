package com.ureka.play4change.prompt


import com.ureka.play4change.domain.AudienceLevel
import com.ureka.play4change.model.ErrorPattern
import com.ureka.play4change.model.StruggleContext

// ─────────────────────────────────────────────────────────────────────────────
//  StruggleAnalysisPrompt
//
//  Builds prompts for adaptive branch generation.
//  The prompt is richer than task generation — it includes the specific
//  struggle context so the AI generates targeted remediation tasks,
//  not just generic tasks on the same topic.
//
//  Key design: the prompt tells the AI exactly what the user struggles with
//  and what the correct path looks like, so subtasks scaffold toward it.
// ─────────────────────────────────────────────────────────────────────────────
object StruggleAnalysisPrompt {

    fun system(): String = """
        You are an expert adaptive learning coach. A student is struggling with 
        a specific task in their learning journey. Your job is to create a short 
        sequence of simpler subtasks that scaffold the student back to success.
        
        PRINCIPLES:
        - Diagnose the root cause of the struggle from the error pattern
        - Start with the simplest possible related concept
        - Each subtask builds on the previous one
        - The final subtask should bring the student back to the original task level
        - Be encouraging — frame tasks as achievable steps, not remediation
        - Keep each subtask completable in 3-10 minutes
        
        OUTPUT FORMAT: Respond with a valid JSON array only. No preamble. No explanation.
        Schema:
        [
          {
            "title": "string (max 60 chars)",
            "description": "string (max 300 chars)",
            "hint": "string (max 150 chars)",
            "pointsReward": number (5-50, lower than main tasks to reflect smaller scope)
          }
        ]
    """.trimIndent()

    fun user(context: StruggleContext, subtaskCount: Int = 3): String = """
        A student is struggling with the following task after ${context.attemptCount} attempts:
        
        Task: ${context.taskDescription}
        Subject Domain: ${context.subjectDomain}
        Module Objective: ${context.moduleObjective}
        Student Level: ${context.audienceLevel.toPromptDescription()}
        Language: ${context.language}
        
        Diagnosed Struggle Type: ${context.errorPattern.toPromptDescription()}
        
        Generate $subtaskCount adaptive subtasks that:
        1. Address the specific struggle type diagnosed above
        2. Start simpler than the original task
        3. Progressively build back toward the original task level
        4. Are encouraging and reframe the struggle as a learning opportunity
        
        Generate the JSON array now:
    """.trimIndent()

    private fun AudienceLevel.toPromptDescription(): String = when (this) {
        AudienceLevel.BEGINNER -> "Beginner — needs very concrete, simple explanations"
        AudienceLevel.INTERMEDIATE -> "Intermediate — can handle some abstraction"
        AudienceLevel.ADVANCED -> "Advanced — understands concepts but needs edge case clarity"
    }

    private fun ErrorPattern.toPromptDescription(): String = when (this) {
        ErrorPattern.CONCEPTUAL_MISUNDERSTANDING ->
            "The student misunderstands the core concept. Start by re-explaining it differently."
        ErrorPattern.PROCEDURAL_ERROR ->
            "The student understands the concept but applies the wrong steps. Focus on procedure."
        ErrorPattern.KNOWLEDGE_GAP ->
            "The student is missing prerequisite knowledge. Fill the gap before returning to the task."
        ErrorPattern.UNCLEAR_INSTRUCTIONS ->
            "The task instructions were unclear. Provide clearer, more explicit guidance."
        ErrorPattern.UNKNOWN ->
            "The struggle cause is unclear. Provide general scaffolding from basic to intermediate."
    }
}
