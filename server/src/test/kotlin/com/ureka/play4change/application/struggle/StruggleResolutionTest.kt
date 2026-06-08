package com.ureka.play4change.application.struggle

import com.ureka.play4change.application.port.SubmitAdaptiveTaskCommand
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.Enrollment
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.struggle.AdaptiveTask
import com.ureka.play4change.domain.struggle.ErrorPattern
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.struggle.StruggleSession
import com.ureka.play4change.domain.struggle.StruggleStatus
import com.ureka.play4change.application.port.ExplanationUseCase
import com.ureka.play4change.domain.topic.TaskType
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class StruggleResolutionTest {

    private val struggleRepository = mockk<StruggleRepository>()
    private val enrollmentRepository = mockk<EnrollmentRepository>()
    private val handleStruggleService = mockk<HandleStruggleService>(relaxed = true)
    private val explanationService = mockk<ExplanationUseCase>(relaxed = true)
    private val registry = mockk<MeterRegistry>(relaxed = true)

    private val service = AdaptiveTaskService(struggleRepository, enrollmentRepository, handleStruggleService, explanationService, registry)

    private val userId = "user-1"
    private val enrollmentId = "enrollment-1"
    private val sessionId = "session-1"
    private val originalAssignmentId = "original-assignment-1"

    private val enrollment = Enrollment(
        id = enrollmentId, userId = userId, topicId = "topic-1",
        topicModuleId = "module-1",
        enrolledAt = OffsetDateTime.now(ZoneOffset.UTC),
        status = EnrollmentStatus.ACTIVE,
        currentDayIndex = 0, totalPointsEarned = 0, streakDays = 0, lastActivityAt = null
    )

    private val originalAssignment = TaskAssignment(
        id = originalAssignmentId, enrollmentId = enrollmentId, userId = userId,
        taskTemplateId = "template-1", taskTemplateVersion = 1,
        taskType = TaskType.MULTIPLE_CHOICE,
        assignedAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2),
        dueAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(22),
        submittedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(30),
        status = AssignmentStatus.SUBMITTED,
        selectedOption = 1, isCorrect = false, pointsAwarded = 0,
        optionOrder = listOf(0, 1, 2), wrongAttemptCount = 1, photoUrl = null
    )

    private fun makeAdaptiveTask(
        id: String,
        orderIndex: Int,
        completed: Boolean = false
    ) = AdaptiveTask(
        id = id,
        struggleSessionId = sessionId,
        branchId = null,
        title = "Adaptive task $orderIndex",
        description = "Description",
        hint = null,
        pointsReward = 10,
        orderIndex = orderIndex,
        completedAt = if (completed) OffsetDateTime.now(ZoneOffset.UTC) else null,
        isCorrect = if (completed) true else null,
        options = listOf("Option A", "Option B", "Option C"),
        correctAnswer = 0,
        selectedOption = if (completed) 0 else null,
        optionOrder = listOf(0, 1, 2)
    )

    private fun makeSession(tasks: List<AdaptiveTask>, status: StruggleStatus = StruggleStatus.OPEN) =
        StruggleSession(
            id = sessionId,
            enrollmentId = enrollmentId,
            originalTaskAssignmentId = originalAssignmentId,
            errorPattern = ErrorPattern.WRONG_CONCEPT,
            attemptCount = 2,
            detectedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(30),
            resolvedAt = null,
            status = status,
            adaptiveTasks = tasks
        )

    @Test
    fun `completing 2 of 3 adaptive tasks does not resolve the session`() {
        // task1 done, task2 NOT done, task3 NOT done — submitting task2 leaves task3 pending
        val task1 = makeAdaptiveTask("task-1", 0, completed = true)
        val task2 = makeAdaptiveTask("task-2", 1, completed = false)
        val task3 = makeAdaptiveTask("task-3", 2, completed = false)
        val session = makeSession(listOf(task1, task2, task3))

        every { struggleRepository.findById(sessionId) } returns session
        every { enrollmentRepository.findById(enrollmentId) } returns enrollment
        every { struggleRepository.save(any()) } answers { firstArg() }
        every { enrollmentRepository.save(any()) } answers { firstArg() }

        val result = service.submitAdaptiveTask(
            SubmitAdaptiveTaskCommand(userId, sessionId, "task-2", selectedOption = 0)
        )

        // Session should NOT be resolved — task3 still pending
        assertFalse(result.getOrNull()!!.sessionResolved)

        val savedSession = slot<StruggleSession>()
        verify { struggleRepository.save(capture(savedSession)) }
        assertEquals(StruggleStatus.OPEN, savedSession.captured.status)

        // Original assignment must NOT be reset — no saveAssignment call
        verify(exactly = 0) { enrollmentRepository.saveAssignment(any()) }
    }

    @Test
    fun `completing all 3 adaptive tasks resolves the session`() {
        val task1 = makeAdaptiveTask("task-1", 0, completed = true)
        val task2 = makeAdaptiveTask("task-2", 1, completed = true)
        val task3 = makeAdaptiveTask("task-3", 2, completed = false) // last one not done

        val session = makeSession(listOf(task1, task2, task3))

        every { struggleRepository.findById(sessionId) } returns session
        every { enrollmentRepository.findById(enrollmentId) } returns enrollment
        every { struggleRepository.save(any()) } answers { firstArg() }
        every { enrollmentRepository.save(any()) } answers { firstArg() }
        every { enrollmentRepository.findAssignmentById(originalAssignmentId) } returns originalAssignment
        every { enrollmentRepository.saveAssignment(any()) } answers { firstArg() }

        val result = service.submitAdaptiveTask(
            SubmitAdaptiveTaskCommand(userId, sessionId, "task-3", selectedOption = 0)
        )

        assertTrue(result.getOrNull()!!.sessionResolved)

        val savedSession = slot<StruggleSession>()
        verify { struggleRepository.save(capture(savedSession)) }
        assertEquals(StruggleStatus.RESOLVED, savedSession.captured.status)
    }

    @Test
    fun `failed adaptive tasks at depth 1 escalate immediately to explanation`() {
        val task1 = makeAdaptiveTask("task-1", 0, completed = true).copy(isCorrect = false)
        val task2 = makeAdaptiveTask("task-2", 1, completed = true).copy(isCorrect = false)
        val task3 = makeAdaptiveTask("task-3", 2, completed = false)
        val session = makeSession(listOf(task1, task2, task3))

        every { struggleRepository.findById(sessionId) } returns session
        every { enrollmentRepository.findById(enrollmentId) } returns enrollment
        every { struggleRepository.save(any()) } answers { firstArg() }
        every { enrollmentRepository.save(any()) } answers { firstArg() }
        every { enrollmentRepository.findAssignmentById(originalAssignmentId) } returns originalAssignment
        every { enrollmentRepository.saveAssignment(any()) } answers { firstArg() }
        every {
            struggleRepository.countByEnrollmentIdAndOriginalAssignmentId(enrollmentId, originalAssignmentId)
        } returns 1
        every { explanationService.createSession(any(), any(), any()) } returns "explanation-session-1"

        val result = service.submitAdaptiveTask(
            SubmitAdaptiveTaskCommand(userId, sessionId, "task-3", selectedOption = 1)
        )

        assertEquals("explanation-session-1", result.getOrNull()!!.explanationSessionId)
        verify(exactly = 1) { explanationService.createSession(enrollmentId, originalAssignmentId, any()) }
        verify(exactly = 0) { handleStruggleService.triggerFromPreviousSession(any(), any()) }
    }

    @Test
    fun `after session resolution the original task assignment is reset to PENDING for retry`() {
        val task1 = makeAdaptiveTask("task-1", 0, completed = true)
        val task2 = makeAdaptiveTask("task-2", 1, completed = true)
        val task3 = makeAdaptiveTask("task-3", 2, completed = false)
        val session = makeSession(listOf(task1, task2, task3))

        every { struggleRepository.findById(sessionId) } returns session
        every { enrollmentRepository.findById(enrollmentId) } returns enrollment
        every { struggleRepository.save(any()) } answers { firstArg() }
        every { enrollmentRepository.save(any()) } answers { firstArg() }
        every { enrollmentRepository.findAssignmentById(originalAssignmentId) } returns originalAssignment
        every { enrollmentRepository.saveAssignment(any()) } answers { firstArg() }

        service.submitAdaptiveTask(
            SubmitAdaptiveTaskCommand(userId, sessionId, "task-3", selectedOption = 0)
        )

        val resetSlot = slot<TaskAssignment>()
        verify { enrollmentRepository.saveAssignment(capture(resetSlot)) }
        with(resetSlot.captured) {
            assertEquals(originalAssignmentId, id)
            assertEquals(AssignmentStatus.PENDING, status)
            // wrongAttemptCount is intentionally preserved on reset — failure history
            // must survive so stats remain accurate when the learner retries.
            assertEquals(1, wrongAttemptCount)
            assertEquals(null, submittedAt)
            assertEquals(null, isCorrect)
            assertEquals(0, pointsAwarded)
        }
    }
}
