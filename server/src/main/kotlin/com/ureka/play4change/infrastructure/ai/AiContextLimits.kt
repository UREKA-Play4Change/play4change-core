package com.ureka.play4change.infrastructure.ai

/**
 * Hard limits on how much text is forwarded to the AI generation layer.
 *
 * Centralised here so every call-site uses the same values and they can be
 * tuned in one place without hunting across multiple services.
 *
 * All values are in characters (not tokens); the caller is responsible for
 * calling [String.take] with the appropriate constant before building its prompt.
 */
object AiContextLimits {
    /** Maximum raw extracted content forwarded per generation request. */
    const val CONTENT_CHARS = 8_000

    /** Maximum topic/module description used as the module objective. */
    const val DESCRIPTION_CHARS = 500

    /** Maximum subject-domain text forwarded in a struggle-session context. */
    const val STRUGGLE_CONTEXT_CHARS = 2_000

    /** Maximum module objective text forwarded per request. */
    const val OBJECTIVE_CHARS = 500
}
