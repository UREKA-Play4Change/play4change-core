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
    val embedding: FloatArray,       // pgvector embedding for deduplication
    val status: GenerationStatus
)

data class GenerationMetadata(
    val tasksRequested: Int,
    val tasksGenerated: Int,
    val tasksDeduplicated: Int,      // skipped due to semantic similarity
    val tokensUsed: Long,
    val generationTimeMs: Long,
    val providerName: String         // e.g. "mistral", "openai" — for observability
)

enum class GenerationStatus {
    SUCCESS,
    FAILED,
    DUPLICATE   // skipped by pgvector deduplication
}