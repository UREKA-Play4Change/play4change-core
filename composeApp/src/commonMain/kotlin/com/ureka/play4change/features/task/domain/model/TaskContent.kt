package com.ureka.play4change.features.task.domain.model

sealed class TaskContent {

    data class QuizContent(
        val questions: List<Question>
    ) : TaskContent()

    data class StepContent(
        val steps: List<TaskStep>
    ) : TaskContent()
}

data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

data class TaskStep(
    val id: String,
    val index: Int,
    val instruction: String,
    val requiresPhoto: Boolean
)
