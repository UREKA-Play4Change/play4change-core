package com.ureka.play4change.prompt

import com.ureka.play4change.domain.AudienceLevel
import com.ureka.play4change.model.ConversationMessage
import com.ureka.play4change.model.ErrorPattern
import com.ureka.play4change.model.ExplanationContext

// ─────────────────────────────────────────────────────────────────────────────
//  ExplanationPrompt
//
//  Builds prompts for the explanation mode: the holistic explanation generated
//  after a learner exhausts all struggle-path depth levels, and the
//  conversational replies when the learner still feels confused.
// ─────────────────────────────────────────────────────────────────────────────
object ExplanationPrompt {

    fun systemExplanation(): String = """
        You are a patient and empathetic learning coach helping a student who has
        struggled repeatedly with the same concept. They have now exhausted all
        their practice attempts and need a clear, holistic explanation.

        YOUR GOAL:
        - Synthesise what went wrong across all their attempts
        - Explain the underlying concept clearly and simply
        - Use concrete examples relevant to the subject domain
        - Be encouraging — frame struggles as a normal part of learning
        - Keep the explanation concise: 3-5 paragraphs maximum
        - Write in plain prose — no JSON, no bullet lists, no headers
        - Match the learner's language exactly
    """.trimIndent()

    fun userExplanation(context: ExplanationContext): String = """
        A student in a ${context.audienceLevel.toLabel()} course on "${context.subjectDomain}" has
        struggled with the following task after exhausting their practice attempts:

        Task: ${context.taskDescription}
        Module objective: ${context.moduleObjective}
        Struggle type: ${context.errorPattern.toLabel()}
        Language: ${context.language}

        Here is their struggle history:
        ${context.struggleHistory.joinToString("\n") { s ->
            "  Session ${s.depth} (${s.errorPattern.toLabel()}): ${s.taskTitles.joinToString(", ")}"
        }}

        Write a clear, encouraging explanation that helps them finally understand this concept.
        Respond in ${context.language}. Plain prose only.
    """.trimIndent()

    fun systemReply(): String = """
        You are a patient learning coach in an ongoing conversation with a confused student.
        They have already received an initial explanation of a concept they struggled with.
        Your role now is to answer their specific questions and clear up remaining confusion.

        PRINCIPLES:
        - Address their exact question directly
        - Use simple language and concrete examples
        - Keep replies focused: 2-4 sentences unless the question demands more
        - Be warm and encouraging
        - Write in the same language as the student
        - Plain prose only — no JSON, no bullet lists
    """.trimIndent()

    fun userReply(
        context: ExplanationContext,
        history: List<ConversationMessage>,
        userMessage: String
    ): String = buildString {
        appendLine("Subject: ${context.subjectDomain}")
        appendLine("Original task: ${context.taskDescription}")
        appendLine()
        appendLine("Conversation so far:")
        history.forEach { msg ->
            appendLine("${if (msg.role == "AI") "Coach" else "Student"}: ${msg.content}")
        }
        appendLine()
        appendLine("Student: $userMessage")
        appendLine()
        append("Coach (respond in ${context.language}):")
    }

    private fun AudienceLevel.toLabel(): String = when (this) {
        AudienceLevel.BEGINNER     -> "beginner"
        AudienceLevel.INTERMEDIATE -> "intermediate"
        AudienceLevel.ADVANCED     -> "advanced"
    }

    private fun ErrorPattern.toLabel(): String = when (this) {
        ErrorPattern.CONCEPTUAL_MISUNDERSTANDING -> "conceptual misunderstanding"
        ErrorPattern.PROCEDURAL_ERROR            -> "procedural error"
        ErrorPattern.KNOWLEDGE_GAP               -> "knowledge gap"
        ErrorPattern.UNCLEAR_INSTRUCTIONS        -> "unclear instructions"
        ErrorPattern.TIME_PRESSURE               -> "time pressure"
        ErrorPattern.UNKNOWN                     -> "unclear struggle"
    }
}
