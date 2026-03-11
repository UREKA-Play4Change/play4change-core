package com.ureka.play4change.prompt


import com.ureka.play4change.domain.AudienceLevel
import com.ureka.play4change.model.GenerationRequest

// ─────────────────────────────────────────────────────────────────────────────
//  TaskGenerationPrompt
//
//  Builds structured prompts for fixed path task generation.
//  Isolated here so prompts can be versioned, tested, and changed
//  independently of the LangChain4j wiring.
//
//  Prompt engineering decisions:
//  - System prompt sets the persona and output contract
//  - User prompt is course-aware (domain, level, objective)
//  - JSON output schema is explicit to prevent hallucinated structure
//  - Temperature kept low (0.7) for consistent educational content
// ─────────────────────────────────────────────────────────────────────────────
object TaskGenerationPrompt {

    fun system(): String = """
        You are an expert educational content designer specialising in gamified 
        learning experiences. You create engaging daily tasks that teach real skills 
        through practice, not memorisation.
        
        RULES:
        - Each task must be completable in 5-15 minutes
        - Tasks must be practical and actionable, not theoretical
        - Difficulty must match the audience level exactly
        - Hints must guide without giving away the answer
        - Every task must directly advance the module objective
        - Tasks must be distinct — no semantic overlap with existing content
        
        OUTPUT FORMAT: Respond with a valid JSON array only. No preamble. No explanation.
        Schema:
        [
          {
            "title": "string (max 60 chars)",
            "description": "string (max 300 chars)",
            "hint": "string (max 150 chars)",
            "pointsReward": number (10-100, higher for harder tasks)
          }
        ]
    """.trimIndent()

    fun user(request: GenerationRequest): String = """
        Generate ${request.taskCount} learning tasks for the following context:
        
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