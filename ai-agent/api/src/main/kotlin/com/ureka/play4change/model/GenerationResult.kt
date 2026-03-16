package com.ureka.play4change.model

data class GenerationResult(
    val tasks: List<GeneratedTask>,
    val metadata: GenerationMetadata
)

data class GeneratedTask(
    val externalId: String,
    val title: String,
    val description: String,
    val hint: String,
    val pointsReward: Int,
    val embedding: FloatArray,
    val status: GenerationStatus,
    val optionsJson: String? = null,       // e.g. ["correct","wrong1","wrong2","wrong3"]
    val correctAnswerIndex: Int? = 0       // always 0 — system shuffles before showing user
)

data class GenerationMetadata(
    val tasksRequested: Int,
    val tasksGenerated: Int,
    val tasksDeduplicated: Int,
    val tokensUsed: Long,
    val generationTimeMs: Long,
    val providerName: String
)

enum class GenerationStatus {
    SUCCESS,
    FAILED,
    DUPLICATE
}