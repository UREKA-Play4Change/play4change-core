package com.ureka.play4change.application.struggle

import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.struggle.ErrorPattern
import com.ureka.play4change.domain.topic.TaskTemplate
import java.time.OffsetDateTime
import kotlin.math.abs

object ErrorPatternClassifier {

    /**
     * Called at the moment of the 2nd wrong answer submission.
     *
     * @param assignment  the persisted assignment — selectedOption holds the FIRST wrong answer
     * @param newSelectedOption  the shuffled index being submitted now (second wrong)
     * @param template    the task template — needed for correctAnswer (original index)
     */
    fun classify(
        assignment: TaskAssignment,
        newSelectedOption: Int,
        template: TaskTemplate
    ): ErrorPattern {
        // 1. TIME_PRESSURE — submitted after the 24-hour window
        if (OffsetDateTime.now().isAfter(assignment.dueAt))
            return ErrorPattern.TIME_PRESSURE

        // 2. READING_ERROR — selected option is adjacent to correct in the original (unshuffled) array
        val correctOriginalIndex = template.correctAnswer
        if (correctOriginalIndex != null) {
            val selectedOriginalIndex = assignment.optionOrder[newSelectedOption]
            if (abs(selectedOriginalIndex - correctOriginalIndex) <= 1)
                return ErrorPattern.READING_ERROR
        }

        // 3. PARTIAL_UNDERSTANDING — user tried two different wrong options
        if (assignment.selectedOption != null && assignment.selectedOption != newSelectedOption)
            return ErrorPattern.PARTIAL_UNDERSTANDING

        // 4. WRONG_CONCEPT — default
        return ErrorPattern.WRONG_CONCEPT
    }
}
