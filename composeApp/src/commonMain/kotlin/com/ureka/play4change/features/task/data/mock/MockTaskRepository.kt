package com.ureka.play4change.features.task.data.mock

import com.ureka.play4change.features.task.domain.model.SubmitResult
import com.ureka.play4change.features.task.domain.model.TaskDetail
import com.ureka.play4change.features.task.domain.repository.TaskRepository
import kotlinx.coroutines.delay

class MockTaskRepository : TaskRepository {
    private val tasks = mapOf(
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
            ?: tasks.values.first() // fallback for demo
    }

    override suspend fun submitAnswer(userTaskId: String, selectedIndex: Int): SubmitResult {
        delay(400)
        val task = tasks[userTaskId] ?: tasks.values.first()
        val isCorrect = selectedIndex == task.correctIndex
        return SubmitResult(isCorrect = isCorrect, pointsAwarded = if (isCorrect) task.pointsReward else 0)
    }
}
