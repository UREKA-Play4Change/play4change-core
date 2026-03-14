package com.ureka.play4change.services

import com.ureka.play4change.domain.CreateCourseRequest
import com.ureka.play4change.domain.CreateCourseResponse
import com.ureka.play4change.port.TaskGenerationPort
import com.ureka.play4change.repo.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val moduleRepository: CourseModuleRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val taskGenerationPort: TaskGenerationPort  // add this
){

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createCourse(request: CreateCourseRequest): CreateCourseResponse {

        // 1. Persist course
        val course = courseRepository.save(
            CourseEntity(
                title = request.title,
                subjectDomain = request.subjectDomain
            )
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

        // 3. Seed mock task pool — 3 tasks per day
        // TODO: replace with taskGenerationPort.generateTasks() once Mistral key is set
        var tasksSeeded = 0
        for (dayIndex in 0 until request.durationDays) {
            for (poolIndex in 0 until 3) {
                taskTemplateRepository.save(
                    TaskTemplateEntity(
                        module = module,
                        dayIndex = dayIndex,
                        poolIndex = poolIndex,
                        title = "Day ${dayIndex + 1} · Question ${poolIndex + 1}",
                        description = "Mock task for ${request.subjectDomain}. Day ${dayIndex + 1}, variant ${poolIndex + 1}.",
                        hint = "Think about the core principles of ${request.subjectDomain}.",
                        taskType = "MULTIPLE_CHOICE",
                        pointsReward = 20,
                        options = """["Option A", "Option B", "Option C", "Option D"]""",
                        correctAnswer = 0
                    )
                )
                tasksSeeded++
            }
        }

        log.info("Created course={} module={} tasksSeeded={}", course.id, module.id, tasksSeeded)

        return CreateCourseResponse(
            courseId = course.id,
            moduleId = module.id,
            title = course.title,
            durationDays = request.durationDays,
            tasksSeeded = tasksSeeded
        )
    }
}