package com.ureka.play4change.application.topic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AiOutputSanitiserTest {

    @Test
    fun `html tags are stripped from ai output fields`() {
        assertEquals("hello world", AiOutputSanitiser.sanitise("<b>hello</b> world"))
    }

    @Test
    fun `script tags are stripped`() {
        assertEquals("", AiOutputSanitiser.sanitise("<script>alert('xss')</script>"))
    }

    @Test
    fun `plain text is preserved unchanged`() {
        assertEquals("plain text", AiOutputSanitiser.sanitise("plain text"))
    }

    @Test
    fun `nested html is fully stripped`() {
        assertEquals(
            "What is the capital of France?",
            AiOutputSanitiser.sanitise("<p><strong>What is the capital of France?</strong></p>")
        )
    }

    @Test
    fun `html entities are decoded`() {
        val result = AiOutputSanitiser.sanitise("A &amp; B")
        assertEquals("A & B", result)
    }
}
