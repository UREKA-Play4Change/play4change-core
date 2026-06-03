package com.ureka.play4change.web.admin

import com.ureka.play4change.application.port.AdminTaskUseCase
import com.ureka.play4change.application.port.UpdateTaskCommand
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.admin.dto.AdaptiveTaskAdminResponse
import com.ureka.play4change.web.admin.dto.TaskTemplateAdminResponse
import com.ureka.play4change.web.admin.dto.UpdateTaskRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin")
class AdminTaskController(private val adminTaskUseCase: AdminTaskUseCase) {

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

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
