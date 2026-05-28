package com.ureka.play4change.features.task.domain.repository

import com.ureka.play4change.features.task.domain.model.SubmitResult
import com.ureka.play4change.features.task.domain.model.TaskDetail

interface TaskRepository {
    suspend fun getTask(userTaskId: String): TaskDetail
    suspend fun submitAnswer(userTaskId: String, selectedIndex: Int): SubmitResult
    suspend fun uploadPhoto(localPath: String): String
    suspend fun submitPhoto(taskId: String, photoUrl: String): SubmitResult
}
