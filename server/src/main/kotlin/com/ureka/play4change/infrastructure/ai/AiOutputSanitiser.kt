package com.ureka.play4change.infrastructure.ai

import org.jsoup.Jsoup

/**
 * Strips all HTML from AI-generated text fields before persistence.
 *
 * Mistral may return HTML tags or entities in generated content, either from
 * prompt injection in the ingested URL or from model behaviour. Passing all
 * generated strings through this sanitiser prevents XSS when content is later
 * rendered in a web context (OWASP A03).
 *
 * Uses Jsoup.parse().text() which strips all tags, decodes HTML entities, and
 * returns plain text suitable for DB storage. Script element content is not
 * included in jsoup's text output, providing defence against injected scripts.
 *
 * Lives in `infrastructure.ai` (not `application.topic`) because sanitising raw AI
 * output is an infrastructure concern — it compensates for a characteristic of the
 * AI provider, not for business logic.
 */
object AiOutputSanitiser {
    fun sanitise(input: String): String = Jsoup.parse(input).text()
}
