package com.ureka.play4change.port

import arrow.core.Either
import com.ureka.play4change.error.AppError
import com.ureka.play4change.model.AdaptiveBranch
import com.ureka.play4change.model.ConversationMessage
import com.ureka.play4change.model.ExplanationContext
import com.ureka.play4change.model.GenerationRequest
import com.ureka.play4change.model.GenerationResult
import com.ureka.play4change.model.StruggleContext
/**
 * THE contract between :server and the AI layer.
 * To swap LLM providers: implement this interface, register as @Primary bean.
 * */

interface TaskGenerationPort {

    /**
     * Generate tasks for a fixed course learning path.
     * Called by the nightly batch job for pre-generation,
     * and on-demand when a new course is created.
     *
     * @param request contains course context, domain, audience level, task count
     * @return generated tasks with embeddings for pgvector deduplication
     */
    suspend fun generateTasks(
        request: GenerationRequest
    ): Either<AppError, GenerationResult>

    /**
     * Generate an adaptive branch for a struggling user.
     * Called on-demand when StruggleDetectionService identifies a struggling user.
     *
     * Before generating, the implementation MUST query pgvector for similar
     * past struggles and apply the similarity-based reuse strategy:
     *   similarity > 0.90 → reuse existing branch
     *   similarity > 0.65 → merge existing + generate complement
     *   similarity < 0.65 → generate fresh, store for future reuse
     *
     * @param context struggle context including error pattern and course domain
     * @return adaptive branch with subtasks tailored to the struggle
     */
    suspend fun generateAdaptiveBranch(
        context: StruggleContext
    ): Either<AppError, AdaptiveBranch>

    /**
     * Generate a holistic explanation for a learner who exhausted all struggle depth levels.
     * The explanation synthesises the full struggle history into clear, encouraging prose.
     *
     * @param context explanation context including the full struggle history
     * @return plain-text explanation the learner can read and respond to
     */
    suspend fun generateExplanation(
        context: ExplanationContext
    ): Either<AppError, String>

    /**
     * Generate a conversational reply to a learner's confused message inside an explanation session.
     *
     * @param context the original explanation context
     * @param history conversation so far (alternating USER/AI messages)
     * @param userMessage the learner's latest message
     * @return AI's reply text
     */
    suspend fun generateExplanationReply(
        context: ExplanationContext,
        history: List<ConversationMessage>,
        userMessage: String
    ): Either<AppError, String>

    /**
     * Health check — verifies the LLM provider is reachable.
     * Used by the circuit breaker and Spring Boot actuator health indicator.
     */
    suspend fun healthCheck(): Boolean
}
