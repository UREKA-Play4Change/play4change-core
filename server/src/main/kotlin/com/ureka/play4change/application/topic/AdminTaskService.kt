package com.ureka.play4change.application.topic

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.AdminTaskUseCase
import com.ureka.play4change.application.port.TaskTemplateWithStats
import com.ureka.play4change.application.port.UpdateTaskCommand
import com.ureka.play4change.domain.struggle.AdaptiveTaskAdminView
import com.ureka.play4change.domain.struggle.StrugglePathStats
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.topic.AdminTaskStatsRepository
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.domain.topic.TaskQuestionStats
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.NotFound
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AdminTaskService(
    private val topicRepository: TopicRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val statsRepository: AdminTaskStatsRepository,
    private val struggleRepository: StruggleRepository,
    private val taskInstanceRepository: TaskInstanceRepository,
    private val batchInstanceGenerationService: BatchInstanceGenerationService
) : AdminTaskUseCase {

    private val log = LoggerFactory.getLogger(AdminTaskService::class.java)

    override fun getTasksForTopic(topicId: String): Either<AppError, List<TaskTemplateWithStats>> = either {
        ensureNotNull(topicRepository.findById(topicId)) {
            NotFound.ResourceNotFound("Topic", topicId)
        }
        val templates = taskTemplateRepository.findCurrentByTopicId(topicId)
        val statsMap = statsRepository.getStatsByTemplateIds(templates.map { it.id })
        templates.map { template ->
            TaskTemplateWithStats(
                template = template,
                stats = statsMap[template.id] ?: TaskQuestionStats.ZERO
            )
        }
    }

    override fun getStruggleTasksForTopic(topicId: String): Either<AppError, List<AdaptiveTaskAdminView>> = either {
        ensureNotNull(topicRepository.findById(topicId)) {
            NotFound.ResourceNotFound("Topic", topicId)
        }
        struggleRepository.findAdaptiveTasksByTopicId(topicId)
    }

    override fun getStrugglePathStats(topicId: String): Either<AppError, List<StrugglePathStats>> = either {
        ensureNotNull(topicRepository.findById(topicId)) {
            NotFound.ResourceNotFound("Topic", topicId)
        }
        struggleRepository.findPathStatsByTopicId(topicId)
    }

    override fun updateAdaptiveTask(taskId: String, command: UpdateTaskCommand): Either<AppError, AdaptiveTaskAdminView> = either {
        val existing = ensureNotNull(struggleRepository.findAdaptiveTaskById(taskId)) {
            NotFound.ResourceNotFound("AdaptiveTask", taskId)
        }
        if (command.options != null) {
            ensure(command.options.size >= 2) {
                BadRequest.InvalidField("options", "must have at least 2 options")
            }
            if (command.correctAnswer != null) {
                ensure(command.correctAnswer in command.options.indices) {
                    BadRequest.InvalidField("correctAnswer", "index out of bounds for provided options")
                }
            }
        }
        struggleRepository.saveAdaptiveTask(
            existing.copy(
                title = command.title,
                description = command.description,
                hint = command.hint,
                options = command.options,
                correctAnswer = command.correctAnswer
            )
        )
        log.info("Admin updated adaptive task {}", taskId)
        ensureNotNull(struggleRepository.findAdaptiveTaskViewById(taskId)) {
            NotFound.ResourceNotFound("AdaptiveTask", taskId)
        }
    }

    override fun updateTask(templateId: String, command: UpdateTaskCommand): Either<AppError, TaskTemplate> = either {
        val template = ensureNotNull(taskTemplateRepository.findById(templateId)) {
            NotFound.ResourceNotFound("TaskTemplate", templateId)
        }
        ensure(command.title.isNotBlank()) {
            BadRequest.InvalidField("title", "must not be blank")
        }
        if (command.options != null) {
            ensure(command.options.size >= 2) {
                BadRequest.InvalidField("options", "must have at least 2 options")
            }
            if (command.correctAnswer != null) {
                ensure(command.correctAnswer in command.options.indices) {
                    BadRequest.InvalidField("correctAnswer", "index out of bounds for provided options")
                }
            }
        }
        val updated = template.copy(
            title = command.title,
            description = command.description,
            hint = command.hint,
            options = command.options,
            correctAnswer = command.correctAnswer,
            version = template.version + 1
        )
        taskTemplateRepository.save(updated)
        taskInstanceRepository.deleteByTaskTemplateId(templateId)
        batchInstanceGenerationService.generateAndSave(listOf(updated))
        log.info("Admin updated task template {} (now version {})", templateId, updated.version)
        updated
    }
}
