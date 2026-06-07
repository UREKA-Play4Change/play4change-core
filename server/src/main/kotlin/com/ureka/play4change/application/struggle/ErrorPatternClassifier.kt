package com.ureka.play4change.application.struggle

import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.struggle.ErrorPattern
import com.ureka.play4change.domain.topic.TaskTemplate
import java.time.OffsetDateTime
import kotlin.math.abs

object ErrorPatternClassifier {

    /**
     * Classifies the error pattern at the moment of a wrong answer submission.
     *
     * Note: [assignment.selectedOption] is null here because the assignment has not been
     * marked submitted yet when this is called. The PARTIAL_UNDERSTANDING pattern (user
     * picked a different wrong option on a second attempt) therefore requires a two-attempt
     * state machine that does not exist today; it is intentionally absent from this classifier.
     *
     * @param assignment        the persisted assignment (selectedOption is null at classification time)
     * @param newSelectedOption the shuffled index currently being submitted
     * @param template          the task template — needed for correctAnswer (original index)
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
            val selectedOriginalIndex = assignment.optionOrder.getOrNull(newSelectedOption)
            if (selectedOriginalIndex != null && abs(selectedOriginalIndex - correctOriginalIndex) <= 1)
                return ErrorPattern.READING_ERROR
        }

        // 3. WRONG_CONCEPT — default
        return ErrorPattern.WRONG_CONCEPT
    }
}
