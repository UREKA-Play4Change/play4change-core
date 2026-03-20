package com.ureka.play4change.features.task.data.mock

import com.ureka.play4change.features.task.domain.model.Question
import com.ureka.play4change.features.task.domain.model.SubmitResult
import com.ureka.play4change.features.task.domain.model.TaskContent
import com.ureka.play4change.features.task.domain.model.TaskDetail
import com.ureka.play4change.features.task.domain.model.TaskStep
import com.ureka.play4change.features.task.domain.repository.TaskRepository
import kotlinx.coroutines.delay

class MockTaskRepository : TaskRepository {
    private val tasks = mapOf(
        "task-quiz-001" to TaskDetail(
            userTaskId = "task-quiz-001",
            title = "Recycling Knowledge Check",
            description = "Test your knowledge on recycling and waste management.",
            hint = "Think about what materials can and cannot be recycled in standard bins.",
            domain = "Sustainability",
            pointsReward = 100,
            content = TaskContent.QuizContent(
                questions = listOf(
                    Question("q1", "Which material CANNOT go in a standard recycling bin?",
                        listOf("Plastic bags", "Cardboard", "Glass bottles", "Aluminium cans"), 0),
                    Question("q2", "What percentage of plastic is actually recycled globally?",
                        listOf("Around 9%", "Around 30%", "Around 50%", "Around 70%"), 0),
                    Question("q3", "Which colour bin is typically used for organic waste in Portugal?",
                        listOf("Brown", "Yellow", "Green", "Blue"), 0),
                    Question("q4", "What does the Möbius loop symbol on packaging indicate?",
                        listOf(
                            "The product is made from recycled material",
                            "The product is 100% recyclable",
                            "The product is biodegradable",
                            "The product is plastic-free"
                        ), 0),
                    Question("q5", "Which action has the highest positive environmental impact?",
                        listOf(
                            "Refusing single-use items",
                            "Recycling plastics",
                            "Composting food waste",
                            "Using reusable bags"
                        ), 0)
                )
            )
        ),
        "task-steps-001" to TaskDetail(
            userTaskId = "task-steps-001",
            title = "Home Energy Audit",
            description = "Walk through your home and identify energy waste.",
            hint = "Look for devices left on standby, gaps in window seals, and inefficient lighting.",
            domain = "Sustainability",
            pointsReward = 150,
            content = TaskContent.StepContent(
                steps = listOf(
                    TaskStep("s1", 0, "Check all lights in your home. Note how many are LED vs. incandescent.", false),
                    TaskStep("s2", 1, "Find devices that are on standby (red light visible). Count them.", false),
                    TaskStep("s3", 2, "Check window and door seals for drafts. Place your hand near the edges.", false),
                    TaskStep("s4", 3, "Locate your home's thermostat or heating controls. Note the current setting.", false),
                    TaskStep("s5", 4, "Take a photo of one specific area where you found energy waste.", true)
                )
            )
        ),
        "task-005" to TaskDetail(
            userTaskId = "task-005",
            title = "Circular Economy",
            description = "Which of the following best describes a 'circular economy'?",
            hint = "Think about what happens to a product at the end of its life in a circular vs linear system.",
            options = listOf(
                "Resources are kept in use as long as possible through reuse, repair, and recycling",
                "Goods are produced, used, and then discarded as waste",
                "Economic growth is prioritised over environmental impact",
                "Products are manufactured in circular factory layouts to reduce transport costs"
            ),
            correctIndex = 0,
            pointsReward = 50,
            domain = "Sustainability"
        )
    )

    override suspend fun getTask(userTaskId: String): TaskDetail {
        delay(600)
        return tasks[userTaskId]
            ?: tasks.values.first()
    }

    override suspend fun submitAnswer(userTaskId: String, selectedIndex: Int): SubmitResult {
        delay(400)
        val task = tasks[userTaskId] ?: tasks.values.first()
        val isCorrect = when {
            selectedIndex == -1 -> true // photo task always correct
            task.content is TaskContent.QuizContent -> selectedIndex >= 0
            else -> selectedIndex == task.correctIndex
        }
        return SubmitResult(isCorrect = isCorrect, pointsAwarded = if (isCorrect) task.pointsReward else 0)
    }
}
