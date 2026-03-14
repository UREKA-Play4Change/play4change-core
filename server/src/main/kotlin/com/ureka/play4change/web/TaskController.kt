package com.ureka.play4change.web

import com.ureka.play4change.domain.SubmitRequest
import com.ureka.play4change.domain.SubmitResponse
import com.ureka.play4change.domain.TaskResponse
import com.ureka.play4change.services.TaskService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("play4change/tasks")
class TaskController(
    private val taskService: TaskService
) {

    @GetMapping("/daily")
    fun dailyTask(
        @RequestHeader("X-User-Id") userId: String,
        @RequestParam moduleId: String,
        @RequestParam dayIndex: Int
    ): ResponseEntity<TaskResponse> {
        val task = taskService.getDailyTask(userId, moduleId, dayIndex)
        return ResponseEntity.ok(task)
    }

    @PostMapping("/{userTaskId}/submit")
    fun submitTask(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable userTaskId: String,
        @RequestBody request: SubmitRequest
    ): ResponseEntity<SubmitResponse> {
        val result = taskService.submitTask(userId, userTaskId, request)
        return ResponseEntity.ok(result)
    }
}