package com.ureka.play4change.application.port

import com.ureka.play4change.domain.topic.TaskTemplate

/**
 * Outbound port for batch instance generation.
 * Implementations call the AI provider and return N option-sets per template.
 * The correct-answer position is always preserved from the parent template.
 *
 * Key: templateId → list of N option lists (each list = one variant's options)
 */
interface BatchInstancePort {
    fun generateInstances(
        templates: List<TaskTemplate>,
        instancesPerTask: Int
    ): Map<String, List<List<String>>>
}
