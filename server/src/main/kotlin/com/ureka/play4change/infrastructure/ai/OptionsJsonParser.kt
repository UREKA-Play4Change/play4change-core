package com.ureka.play4change.infrastructure.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses a JSON array of strings produced by the AI generation layer.
 *
 * Returns null if the input is malformed (missing, blank, or not a JSON array of strings)
 * so callers can gracefully fall back to the task template's options.
 *
 * Intentionally side-effect-free: used by [TaskGenerationOrchestrator],
 * [HandleStruggleService], [LanguageGenerationAdapter], and [BatchInstanceAdapter].
 */
object OptionsJsonParser {
    fun parse(json: String): List<String>? = runCatching {
        Json.parseToJsonElement(json).jsonArray.map { it.jsonPrimitive.content }
    }.getOrNull()
}
