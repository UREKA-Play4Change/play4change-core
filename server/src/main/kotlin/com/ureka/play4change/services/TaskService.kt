package com.ureka.play4change.services

import com.ureka.play4change.domain.*
import com.ureka.play4change.repo.TaskTemplateRepository
import com.ureka.play4change.repo.UserTaskEntity
import com.ureka.play4change.repo.UserTaskRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class TaskService(
    private val taskTemplateRepository: TaskTemplateRepository,
    private val userTaskRepository: UserTaskRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    // ── GET daily task — lazy + idempotent ───────────────────────────────────

    @Transactional
    fun getDailyTask(userId: String, moduleId: String, dayIndex: Int): TaskResponse {

        // 1. Check for existing assignment (idempotency guarantee)
        val existing = userTaskRepository.findExistingAssignment(userId, moduleId, dayIndex)
        if (existing != null) {
            log.debug("Returning existing assignment userId={} dayIndex={}", userId, dayIndex)
            return existing.toResponse()
        }

        // 2. Load today's pool
        val pool = taskTemplateRepository.findByModuleIdAndDayIndex(moduleId, dayIndex)
        check(pool.isNotEmpty()) { "No tasks in pool for moduleId=$moduleId dayIndex=$dayIndex" }

        // 3. Pick random task from pool
        val template = pool.random()

        // 4. Generate shuffle permutation (anti-cheating — different A/B/C/D per user)
        val optionOrder = template.options
            ?.let { json.decodeFromString<List<String>>(it) }
            ?.indices?.toMutableList()
            ?.also { it.shuffle() }
            ?: emptyList()

        // 5. Persist assignment
        val userTask = userTaskRepository.save(
            UserTaskEntity(
                userId = userId,
                taskTemplate = template,
                optionOrder = json.encodeToString(optionOrder)
            )
        )

        log.info("Assigned task={} to userId={} dayIndex={}", template.id, userId, dayIndex)
        return userTask.toResponse()
    }

    // ── POST submit ──────────────────────────────────────────────────────────

    @Transactional
    fun submitTask(userId: String, userTaskId: String, request: SubmitRequest): SubmitResponse {

        // 1. Load assignment
        val userTask = userTaskRepository.findById(userTaskId)
            .orElseThrow { IllegalArgumentException("UserTask not found: $userTaskId") }

        check(userTask.userId == userId) { "UserTask does not belong to userId=$userId" }
        check(userTask.status == "PENDING") { "Task already submitted" }

        val template = userTask.taskTemplate

        // 2. Parse stored option order
        val optionOrder = userTask.optionOrder
            ?.let { json.decodeFromString<List<Int>>(it) }
            ?: emptyList()

        // 3. Validate answer based on task type
        val (isCorrect, feedback) = when (template.taskType) {

            "MULTIPLE_CHOICE", "TRUE_FALSE" -> {
                val selected = requireNotNull(request.selectedOption) { "selectedOption required for MCQ" }
                // Map shuffled display index → original options index via stored permutation
                // e.g. optionOrder=[2,0,3,1], user selected 0 (shown as "A") → real index = 2
                val realIndex = optionOrder.getOrNull(selected)
                    ?: error("Invalid option index: $selected")
                Pair(realIndex == template.correctAnswer, null)
            }

            "TODO_ACTION" -> {
                val text = request.textAnswer?.takeIf { it.isNotBlank() }
                    ?: error("textAnswer required for TODO_ACTION")
                log.info("TODO_ACTION submitted (mock AI validation) userId={}", userId)
                // TODO: replace with taskGenerationPort.validateTextAnswer() once Mistral key is set
                Pair(true, "Great effort! AI validation will be enabled soon.")
            }

            else -> error("Unknown task type: ${template.taskType}")
        }

        // 4. Detect late submission
        val isLate = LocalDate.now() > userTask.assignedAt.toLocalDate()

        // 5. Calculate points — half if late
        val points = if (isCorrect) {
            if (isLate) template.pointsReward / 2 else template.pointsReward
        } else 0

        val status = if (isLate) "LATE" else "COMPLETED"

        // 6. Persist result
        userTask.submittedAt = OffsetDateTime.now()
        userTask.status = status
        userTask.selectedOption = request.selectedOption
        userTask.textAnswer = request.textAnswer
        userTask.isCorrect = isCorrect
        userTask.pointsAwarded = points
        userTaskRepository.save(userTask)

        log.info("Submitted userTaskId={} correct={} points={} late={}", userTaskId, isCorrect, points, isLate)

        return SubmitResponse(
            userTaskId = userTaskId,
            isCorrect = isCorrect,
            pointsAwarded = points,
            status = TaskStatus.valueOf(status),
            feedback = feedback,
            isLate = isLate
        )
    }

    // ── Entity → Response ─────────────────────────────────────────────────────

    private fun UserTaskEntity.toResponse(): TaskResponse {
        val template = this.taskTemplate
        val order = optionOrder?.let { json.decodeFromString<List<Int>>(it) } ?: emptyList()

        val rawOptions = template.options?.let { json.decodeFromString<List<String>>(it) }

        // Apply shuffle permutation before sending to client
        val shuffledOptions = if (rawOptions != null && order.isNotEmpty()) {
            order.map { rawOptions[it] }
        } else rawOptions

        return TaskResponse(
            userTaskId = this.id,
            title = template.title,
            description = template.description,
            hint = template.hint,
            taskType = TaskType.valueOf(template.taskType),
            pointsReward = template.pointsReward,
            options = shuffledOptions
            // correctAnswer intentionally omitted
        )
    }
}