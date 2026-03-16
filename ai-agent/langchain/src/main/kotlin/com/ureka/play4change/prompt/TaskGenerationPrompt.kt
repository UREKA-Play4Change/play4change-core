package com.ureka.play4change.prompt

import com.ureka.play4change.domain.AudienceLevel
import com.ureka.play4change.model.GenerationRequest

object TaskGenerationPrompt {

    fun system(): String = """
        You are an expert educational content designer specialising in gamified
        learning experiences. You create engaging daily multiple-choice tasks that
        teach real skills through practice, not memorisation.

        RULES:
        - Each task must be completable in 5-15 minutes
        - Tasks must be practical and actionable, not theoretical
        - Difficulty must match the audience level exactly
        - Hints must guide without giving away the answer
        - Every task must directly advance the module objective
        - Tasks must be distinct — no semantic overlap with existing content
        - Always generate exactly 4 options per task
        - The correct answer must always be at index 0 in the options array
          (the system will shuffle before showing to the user)
        - Wrong options must be plausible — not obviously incorrect

        OUTPUT FORMAT: Respond with a valid JSON array only. No preamble. No explanation.
        Schema:
        [
          {
            "title": "string (max 60 chars)",
            "description": "string (max 300 chars — frame it as a question or scenario)",
            "hint": "string (max 150 chars)",
            "pointsReward": number (10-100, higher for harder tasks),
            "options": ["correct answer", "wrong option 1", "wrong option 2", "wrong option 3"],
            "correctAnswerIndex": 0
          }
        ]
    """.trimIndent()

    fun user(request: GenerationRequest): String = """
        Generate ${request.taskCount} multiple-choice learning tasks for the following context:

        Subject Domain: ${request.subjectDomain}
        Module Objective: ${request.moduleObjective}
        Audience Level: ${request.audienceLevel.toPromptDescription()}
        Language: ${request.language}

        The tasks must progress logically — earlier tasks build foundations,
        later tasks apply concepts in more complex scenarios.

        ${if (request.existingEmbeddings.isNotEmpty())
        "Important: ${request.existingEmbeddings.size} tasks already exist for this module. " +
                "Generate semantically distinct tasks that complement the existing content."
    else ""}

        Remember: always put the correct answer at index 0 in the options array.
        Generate the JSON array now:
    """.trimIndent()

    private fun AudienceLevel.toPromptDescription(): String = when (this) {
        AudienceLevel.BEGINNER ->
            "Beginner — assumes no prior knowledge, uses simple language, concrete examples"
        AudienceLevel.INTERMEDIATE ->
            "Intermediate — assumes basic familiarity, introduces complexity, some abstraction"
        AudienceLevel.ADVANCED ->
            "Advanced — assumes solid foundation, focuses on nuance, edge cases, and depth"
    }
}