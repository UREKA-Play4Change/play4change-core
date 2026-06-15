package com.ureka.play4change.web.admin

import com.ureka.play4change.application.port.AdminTaskUseCase
import com.ureka.play4change.application.port.UpdateTaskCommand
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.struggle.StrugglePathStats
import com.ureka.play4change.error.AppError
import com.ureka.play4change.infrastructure.persistence.repository.ExplanationMessageJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.ExplanationSessionJpaRepository
import com.ureka.play4change.web.admin.dto.AdaptiveTaskAdminResponse
import com.ureka.play4change.web.admin.dto.PageResponse
import com.ureka.play4change.web.admin.dto.TaskTemplateAdminResponse
import com.ureka.play4change.web.admin.dto.UpdateTaskRequest
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

data class TopicExplanationResponse(
    val sessionId: String,
    val userId: String,
    val userEmail: String,
    val userName: String?,
    val dayIndex: Int,
    val originalTaskTitle: String,
    val errorPattern: String,
    val status: String,
    val explanationText: String?,
    val generatedAt: OffsetDateTime,
    val resolvedAt: OffsetDateTime?,
    val messages: List<AdminExplanationMessageResponse>
)

@RestController
@RequestMapping("/admin")
class AdminTaskController(
    private val adminTaskUseCase: AdminTaskUseCase,
    private val explanationSessionJpa: ExplanationSessionJpaRepository,
    private val explanationMessageJpa: ExplanationMessageJpaRepository,
    private val userRepository: UserRepository
) {

    @GetMapping("/topics/{topicId}/tasks")
    fun getTopicTasks(
        @PathVariable topicId: String
    ): ResponseEntity<List<TaskTemplateAdminResponse>> =
        adminTaskUseCase.getTasksForTopic(topicId).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(it.map { item -> TaskTemplateAdminResponse.from(item) }) }
        )

    @GetMapping("/topics/{topicId}/struggle-tasks")
    fun getTopicStruggleTasks(
        @PathVariable topicId: String
    ): ResponseEntity<List<AdaptiveTaskAdminResponse>> =
        adminTaskUseCase.getStruggleTasksForTopic(topicId).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(it.map { view -> AdaptiveTaskAdminResponse.from(view) }) }
        )

    @GetMapping("/topics/{topicId}/struggle-path-stats")
    fun getStrugglePathStats(
        @PathVariable topicId: String
    ): ResponseEntity<List<StrugglePathStats>> =
        adminTaskUseCase.getStrugglePathStats(topicId).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(it) }
        )

    @PutMapping("/adaptive-tasks/{taskId}")
    fun updateAdaptiveTask(
        @PathVariable taskId: String,
        @Valid @RequestBody request: UpdateTaskRequest
    ): ResponseEntity<AdaptiveTaskAdminResponse> =
        adminTaskUseCase.updateAdaptiveTask(
            taskId,
            UpdateTaskCommand(
                title = request.title,
                description = request.description,
                hint = request.hint,
                options = request.options,
                correctAnswer = request.correctAnswer
            )
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(AdaptiveTaskAdminResponse.from(it)) }
        )

    @PutMapping("/tasks/{templateId}")
    fun updateTask(
        @PathVariable templateId: String,
        @Valid @RequestBody request: UpdateTaskRequest
    ): ResponseEntity<TaskTemplateAdminResponse> =
        adminTaskUseCase.updateTask(
            templateId,
            UpdateTaskCommand(
                title = request.title,
                description = request.description,
                hint = request.hint,
                options = request.options,
                correctAnswer = request.correctAnswer
            )
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { template ->
                val withStats = com.ureka.play4change.application.port.TaskTemplateWithStats(
                    template = template,
                    stats = com.ureka.play4change.domain.topic.TaskQuestionStats.ZERO
                )
                ResponseEntity.ok(TaskTemplateAdminResponse.from(withStats))
            }
        )

    @GetMapping("/topics/{topicId}/explanations")
    fun getTopicExplanations(
        @PathVariable topicId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<TopicExplanationResponse>> {
        val sessionPage = explanationSessionJpa.findByTopicIdWithDetails(topicId, PageRequest.of(page, size))
        val userIds = sessionPage.content.map { it.enrollment.userId }.distinct()
        val usersById = userIds
            .mapNotNull { uid -> userRepository.findById(uid)?.let { uid to it } }
            .toMap()

        val responses = sessionPage.content.map { session ->
            val user = usersById[session.enrollment.userId]
            val messages = explanationMessageJpa
                .findBySessionIdOrderBySentAtAsc(session.id)
                .map { msg -> AdminExplanationMessageResponse(role = msg.role, content = msg.content, sentAt = msg.sentAt) }
            TopicExplanationResponse(
                sessionId = session.id,
                userId = session.enrollment.userId,
                userEmail = user?.email ?: session.enrollment.userId,
                userName = user?.name,
                dayIndex = session.originalTaskAssignment.taskTemplate.dayIndex,
                originalTaskTitle = session.originalTaskAssignment.taskTemplate.title,
                errorPattern = session.errorPattern,
                status = session.status,
                explanationText = session.explanationText,
                generatedAt = session.generatedAt,
                resolvedAt = session.resolvedAt,
                messages = messages
            )
        }
        return ResponseEntity.ok(
            PageResponse(
                content = responses,
                page = sessionPage.number,
                size = sessionPage.size,
                totalElements = sessionPage.totalElements,
                totalPages = sessionPage.totalPages
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
