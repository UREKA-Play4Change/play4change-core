package com.ureka.play4change.features.struggle.data.mock

import com.ureka.play4change.features.struggle.domain.model.AdaptiveSubmitResult
import com.ureka.play4change.features.struggle.domain.model.AdaptiveTask
import com.ureka.play4change.features.struggle.domain.model.StruggleSession
import com.ureka.play4change.features.struggle.domain.repository.StruggleRepository
import kotlinx.coroutines.delay

class MockStruggleRepository : StruggleRepository {

    override suspend fun getSession(enrollmentId: String): StruggleSession? {
        delay(500)
        return StruggleSession(
            sessionId = "sess-mock-001",
            errorPattern = "WRONG_ANSWER",
            status = "ACTIVE",
            tasks = listOf(
                AdaptiveTask(
                    taskId = "adaptive-mock-001",
                    title = "Adaptive: Recycling",
                    description = "Which item belongs in the recycling bin?",
                    hint = "Think about materials that can be reprocessed.",
                    options = listOf("Glass bottle", "Plastic bag", "Cardboard box", "Food scrap"),
                    pointsReward = 50,
                    isCompleted = false
                )
            )
        )
    }

    override suspend fun submitTask(
        sessionId: String,
        taskId: String,
        selectedOption: Int
    ): AdaptiveSubmitResult {
        delay(400)
        return AdaptiveSubmitResult(
            isCorrect = selectedOption == 0,
            pointsAwarded = if (selectedOption == 0) 50 else 0,
            sessionResolved = true
        )
    }
}
