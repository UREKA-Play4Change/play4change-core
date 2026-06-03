package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.domain.struggle.AdaptiveTaskAdminView
import com.ureka.play4change.domain.struggle.StrugglePathStats
import com.ureka.play4change.domain.topic.TaskQuestionStats
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.error.AppError

data class TaskTemplateWithStats(
    val template: TaskTemplate,
    val stats: TaskQuestionStats
)

data class UpdateTaskCommand(
    val title: String,
    val description: String,
    val hint: String?,
    val options: List<String>?,
    val correctAnswer: Int?
)

interface AdminTaskUseCase {
    fun getTasksForTopic(topicId: String): Either<AppError, List<TaskTemplateWithStats>>
    fun getStruggleTasksForTopic(topicId: String): Either<AppError, List<AdaptiveTaskAdminView>>
    fun getStrugglePathStats(topicId: String): Either<AppError, List<StrugglePathStats>>
    fun updateTask(templateId: String, command: UpdateTaskCommand): Either<AppError, TaskTemplate>
    fun updateAdaptiveTask(taskId: String, command: UpdateTaskCommand): Either<AppError, AdaptiveTaskAdminView>
}
