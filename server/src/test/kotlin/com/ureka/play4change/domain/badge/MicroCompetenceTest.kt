package com.ureka.play4change.domain.badge

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MicroCompetenceTest {

    @Test
    fun `blank name is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MicroCompetence(id = "id", name = "   ", description = "desc", topicId = "topic-1")
        }
    }

    @Test
    fun `empty name is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MicroCompetence(id = "id", name = "", description = "desc", topicId = "topic-1")
        }
    }

    @Test
    fun `blank description is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MicroCompetence(id = "id", name = "Java Basics", description = "   ", topicId = "topic-1")
        }
    }

    @Test
    fun `empty description is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MicroCompetence(id = "id", name = "Java Basics", description = "", topicId = "topic-1")
        }
    }
}
