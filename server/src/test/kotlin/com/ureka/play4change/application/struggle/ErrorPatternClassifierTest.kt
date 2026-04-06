package com.ureka.play4change.application.struggle

import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.struggle.ErrorPattern
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class ErrorPatternClassifierTest {

    // optionOrder is an identity mapping [0,1,2,3]: shuffled index i → original index i
    private fun assignment(
        dueAt: OffsetDateTime = OffsetDateTime.now().plusHours(24),
        optionOrder: List<Int> = listOf(0, 1, 2, 3),
        selectedOption: Int? = null
    ) = TaskAssignment(
        id = "a1",
        enrollmentId = "e1",
        userId = "u1",
        taskTemplateId = "t1",
        taskTemplateVersion = 1,
        taskType = TaskType.MULTIPLE_CHOICE,
        assignedAt = OffsetDateTime.now().minusHours(1),
        dueAt = dueAt,
        submittedAt = null,
        status = AssignmentStatus.PENDING,
        selectedOption = selectedOption,
        isCorrect = null,
        pointsAwarded = 0,
        optionOrder = optionOrder,
        wrongAttemptCount = 1,
        photoUrl = null
    )

    private fun template(correctAnswer: Int? = 2) = TaskTemplate(
        id = "t1",
        moduleId = "m1",
        dayIndex = 0,
        poolIndex = 0,
        title = "Test Task",
        description = "Test",
        hint = null,
        taskType = TaskType.MULTIPLE_CHOICE,
        pointsReward = 20,
        options = listOf("A", "B", "C", "D"),
        correctAnswer = correctAnswer,
        version = 1,
        isCurrent = true,
        supersededBy = null,
        embedding = null,
        createdAt = OffsetDateTime.now()
    )

    // ── WRONG_CONCEPT ─────────────────────────────────────────────────────────

    @Test
    fun `WRONG_CONCEPT - default when no other rule matches`() {
        // optionOrder[0] = 0; correctAnswer = 3; abs(0-3) = 3 > 1 → not READING_ERROR
        // selectedOption = 0 == newSelectedOption = 0           → not PARTIAL_UNDERSTANDING
        // dueAt in future                                        → not TIME_PRESSURE
        val a = assignment(optionOrder = listOf(0, 1, 2, 3), selectedOption = 0)
        val t = template(correctAnswer = 3)
        assertEquals(ErrorPattern.WRONG_CONCEPT, ErrorPatternClassifier.classify(a, 0, t))
    }

    @Test
    fun `WRONG_CONCEPT - when correctAnswer is null the READING_ERROR adjacency check is skipped`() {
        // null correctAnswer → the if-block is never entered → falls through to WRONG_CONCEPT
        val a = assignment(optionOrder = listOf(0, 1, 2, 3), selectedOption = 0)
        val t = template(correctAnswer = null)
        assertEquals(ErrorPattern.WRONG_CONCEPT, ErrorPatternClassifier.classify(a, 0, t))
    }

    // ── READING_ERROR ─────────────────────────────────────────────────────────

    @Test
    fun `READING_ERROR - selected original index is one below correct (abs diff == 1)`() {
        // optionOrder[1] = 1; correctAnswer = 2; abs(1-2) = 1 ≤ 1 → READING_ERROR
        // dueAt in future → TIME_PRESSURE check passes first, then hits READING_ERROR
        val a = assignment(optionOrder = listOf(0, 1, 2, 3))
        val t = template(correctAnswer = 2)
        assertEquals(ErrorPattern.READING_ERROR, ErrorPatternClassifier.classify(a, 1, t))
    }

    @Test
    fun `READING_ERROR - selected original index is one above correct (abs diff == 1)`() {
        // optionOrder[3] = 3; correctAnswer = 2; abs(3-2) = 1 ≤ 1 → READING_ERROR
        val a = assignment(optionOrder = listOf(0, 1, 2, 3))
        val t = template(correctAnswer = 2)
        assertEquals(ErrorPattern.READING_ERROR, ErrorPatternClassifier.classify(a, 3, t))
    }

    // ── PARTIAL_UNDERSTANDING ─────────────────────────────────────────────────

    @Test
    fun `PARTIAL_UNDERSTANDING - two distinct wrong options selected`() {
        // optionOrder[3] = 3; correctAnswer = 0; abs(3-0) = 3 > 1 → not READING_ERROR
        // selectedOption = 1 ≠ newSelectedOption = 3             → PARTIAL_UNDERSTANDING
        val a = assignment(optionOrder = listOf(0, 1, 2, 3), selectedOption = 1)
        val t = template(correctAnswer = 0)
        assertEquals(ErrorPattern.PARTIAL_UNDERSTANDING, ErrorPatternClassifier.classify(a, 3, t))
    }

    // ── TIME_PRESSURE ─────────────────────────────────────────────────────────

    @Test
    fun `TIME_PRESSURE - submitted after dueAt`() {
        val pastDue = OffsetDateTime.now().minusHours(1)
        val a = assignment(dueAt = pastDue)
        val t = template(correctAnswer = 2)
        assertEquals(ErrorPattern.TIME_PRESSURE, ErrorPatternClassifier.classify(a, 0, t))
    }

    // ── Priority ──────────────────────────────────────────────────────────────

    @Test
    fun `TIME_PRESSURE beats READING_ERROR when both conditions apply`() {
        // Past due (TIME_PRESSURE) AND optionOrder[1]=1 adjacent to correctAnswer=2 (READING_ERROR)
        // TIME_PRESSURE is checked first in the classifier → must win
        val pastDue = OffsetDateTime.now().minusHours(1)
        val a = assignment(dueAt = pastDue, optionOrder = listOf(0, 1, 2, 3))
        val t = template(correctAnswer = 2)
        assertEquals(ErrorPattern.TIME_PRESSURE, ErrorPatternClassifier.classify(a, 1, t))
    }

    @Test
    fun `READING_ERROR beats PARTIAL_UNDERSTANDING when both conditions apply`() {
        // optionOrder[1]=1 adjacent to correctAnswer=2  → READING_ERROR condition met
        // selectedOption=0 ≠ newSelectedOption=1         → PARTIAL_UNDERSTANDING also met
        // READING_ERROR is checked before PARTIAL_UNDERSTANDING → must win
        val a = assignment(optionOrder = listOf(0, 1, 2, 3), selectedOption = 0)
        val t = template(correctAnswer = 2)
        assertEquals(ErrorPattern.READING_ERROR, ErrorPatternClassifier.classify(a, 1, t))
    }
}
