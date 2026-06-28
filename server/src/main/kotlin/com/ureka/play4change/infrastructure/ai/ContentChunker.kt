package com.ureka.play4change.infrastructure.ai

/**
 * Splits extracted content into overlapping chunks that fit within the AI context window.
 *
 * Breaks are attempted at paragraph boundaries (\n\n) within the last 500 characters of
 * each window to preserve semantic coherence. Falls back to the hard character limit if
 * no paragraph boundary is found.
 *
 * A single-element list is returned when the text already fits within [chunkSize].
 */
object ContentChunker {

    fun chunk(
        text: String,
        chunkSize: Int = AiContextLimits.CONTENT_CHARS,
        overlap: Int = AiContextLimits.CHUNK_OVERLAP
    ): List<String> {
        if (text.length <= chunkSize) return listOf(text)

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            val rawEnd = minOf(start + chunkSize, text.length)
            val end = if (rawEnd < text.length) {
                val searchFrom = maxOf(rawEnd - 500, start + 1)
                text.lastIndexOf("\n\n", rawEnd).takeIf { it >= searchFrom } ?: rawEnd
            } else {
                rawEnd
            }
            chunks.add(text.substring(start, end))
            val next = end - overlap
            // Guard: ensure forward progress on pathological (no-newline) input.
            if (next <= start) break
            start = next
        }
        return chunks
    }
}
