package com.ureka.play4change.domain.enrollment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AntiCheatShuffleTest {

    @Test
    fun `given same user task and enrollment when shuffleOptions called twice then returns identical order`() {
        val order1 = TaskShuffleSeed.shuffleOptions(4, "user-1", "task-1", "enroll-1")
        val order2 = TaskShuffleSeed.shuffleOptions(4, "user-1", "task-1", "enroll-1")

        assertEquals(order1, order2)
    }

    @Test
    fun `given three different users on same task when shuffled then at least two orders differ`() {
        val orderA = TaskShuffleSeed.shuffleOptions(4, "user-A", "task-1", "enroll-A")
        val orderB = TaskShuffleSeed.shuffleOptions(4, "user-B", "task-1", "enroll-B")
        val orderC = TaskShuffleSeed.shuffleOptions(4, "user-C", "task-1", "enroll-C")

        val distinctOrders = setOf(orderA, orderB, orderC).size
        assertTrue(distinctOrders >= 2, "Expected at least 2 distinct orderings for 3 different users")
    }

    @Test
    fun `given shuffled order when locating canonical correct answer then remapped index points to original`() {
        val canonicalCorrect = 2
        val order = TaskShuffleSeed.shuffleOptions(4, "user-1", "task-x", "enroll-x")

        // Find which shuffled position the canonical correct answer occupies
        val shuffledPosition = order.indexOf(canonicalCorrect)
        // Remapping that position back through optionOrder must return the canonical index
        val remapped = order.getOrNull(shuffledPosition)

        assertNotNull(remapped)
        assertEquals(canonicalCorrect, remapped)
    }

    @Test
    fun `given zero options when shuffleOptions called then returns empty list`() {
        val order = TaskShuffleSeed.shuffleOptions(0, "user-1", "task-1", "enroll-1")

        assertTrue(order.isEmpty())
    }

    @Test
    fun `given shuffle result when used as optionOrder then contains all canonical indices exactly once`() {
        val optionCount = 5
        val order = TaskShuffleSeed.shuffleOptions(optionCount, "user-1", "task-1", "enroll-1")

        assertEquals(optionCount, order.size)
        assertEquals((0 until optionCount).toSet(), order.toSet())
    }
}
