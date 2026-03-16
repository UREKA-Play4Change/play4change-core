package com.ureka.play4change.services

import arrow.core.Either
import com.ureka.play4change.domain.AudienceLevel
import com.ureka.play4change.domain.CreateCourseRequest
import com.ureka.play4change.domain.CreateCourseResponse
import com.ureka.play4change.domain.EnrollResponse
import com.ureka.play4change.error.AppError
import com.ureka.play4change.model.GenerationRequest
import com.ureka.play4change.model.GenerationResult
import com.ureka.play4change.model.GenerationStatus
import com.ureka.play4change.port.TaskGenerationPort
import com.ureka.play4change.repo.CourseEntity
import com.ureka.play4change.repo.CourseModuleEntity
import com.ureka.play4change.repo.CourseModuleRepository
import com.ureka.play4change.repo.CourseRepository
import com.ureka.play4change.repo.TaskTemplateEntity
import com.ureka.play4change.repo.TaskTemplateRepository
import com.ureka.play4change.repo.UserSubscriptionEntity
import com.ureka.play4change.repo.UserSubscriptionRepository
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val moduleRepository: CourseModuleRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val taskGenerationPort: TaskGenerationPort,
    private val subscriptionRepository: UserSubscriptionRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createCourse(request: CreateCourseRequest): CreateCourseResponse {

        // 1. Persist course
        val course = courseRepository.save(
            CourseEntity(title = request.title, subjectDomain = request.subjectDomain)
        )

        // 2. Persist module
        val module = moduleRepository.save(
            CourseModuleEntity(
                course = course,
                title = request.moduleTitle,
                topic = request.subjectDomain,
                objective = request.moduleObjective
            )
        )

        // 3. Generate tasks with Mistral — fallback to mock if AI fails
        var tasksSeeded = 0

        for (dayIndex in 0 until request.durationDays) {
            log.info("Generating tasks for courseId={} dayIndex={}", course.id, dayIndex)

            val aiResult: Either<AppError, GenerationResult> = runBlocking {
                taskGenerationPort.generateTasks(
                    GenerationRequest(
                        courseId = course.id,
                        moduleId = module.id,
                        subjectDomain = request.subjectDomain,
                        audienceLevel = AudienceLevel.BEGINNER,
                        language = "en",
                        taskCount = 3,
                        moduleObjective = request.moduleObjective
                    )
                )
            }

            aiResult.fold(
                ifLeft = { error ->
                    // AI failed — fall back to mock so course creation still succeeds
                    log.warn("AI generation failed for dayIndex={} error={} — using mock fallback", dayIndex, error)
                    for (poolIndex in 0 until 3) {
                        taskTemplateRepository.save(mockTask(module, dayIndex, poolIndex, request.subjectDomain))
                        tasksSeeded++
                    }
                },
                ifRight = { result ->
                    // AI succeeded — persist real tasks
                    result.tasks
                        .filter { it.status == GenerationStatus.SUCCESS }
                        .forEachIndexed { poolIndex, task ->
                            taskTemplateRepository.save(
                                TaskTemplateEntity(
                                    module = module,
                                    dayIndex = dayIndex,
                                    poolIndex = poolIndex,
                                    title = task.title,
                                    description = task.description,
                                    hint = task.hint,
                                    taskType = "MULTIPLE_CHOICE",
                                    pointsReward = task.pointsReward,
                                    options = task.optionsJson,
                                    correctAnswer = task.correctAnswerIndex ?: 0
                                )
                            )
                            tasksSeeded++
                        }

                    log.info(
                        "AI generated dayIndex={} tasks={} duplicatesSkipped={} tokens={} ms={}",
                        dayIndex,
                        result.metadata.tasksGenerated,
                        result.metadata.tasksDeduplicated,
                        result.metadata.tokensUsed,
                        result.metadata.generationTimeMs
                    )
                }
            )
        }

        log.info("Course created courseId={} moduleId={} tasksSeeded={}", course.id, module.id, tasksSeeded)

        return CreateCourseResponse(
            courseId = course.id,
            moduleId = module.id,
            title = course.title,
            durationDays = request.durationDays,
            tasksSeeded = tasksSeeded
        )
    }

    @Transactional
    fun enrollUser(userId: String, courseId: String): EnrollResponse {
        val course = courseRepository.findById(courseId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found")
        }
        val module = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Course has no modules")

        val subscription = subscriptionRepository.save(
            UserSubscriptionEntity(userId = userId, course = course, module = module)
        )

        return EnrollResponse(subscriptionId = subscription.id, enrolledAt = subscription.enrolledAt)
    }

    private fun mockTask(
        module: CourseModuleEntity,
        dayIndex: Int,
        poolIndex: Int,
        domain: String
    ) = TaskTemplateEntity(
        module = module,
        dayIndex = dayIndex,
        poolIndex = poolIndex,
        title = "Day ${dayIndex + 1} · Question ${poolIndex + 1}",
        description = "Mock task for $domain. Day ${dayIndex + 1}, variant ${poolIndex + 1}.",
        hint = "Think about the core principles of $domain.",
        taskType = "MULTIPLE_CHOICE",
        pointsReward = 20,
        options = """["Option A", "Option B", "Option C", "Option D"]""",
        correctAnswer = 0
    )
}