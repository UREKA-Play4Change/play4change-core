package com.ureka.play4change.application.enrollment

import com.ureka.play4change.application.port.LanguageGenerationPort
import com.ureka.play4change.config.LanguageProperties
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import org.springframework.stereotype.Service

@Service
class LanguageGatingService(
    private val properties: LanguageProperties,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val generationPort: LanguageGenerationPort
) {
    /**
     * Resolves which task template to serve for a given user language preference.
     *
     * - If preferredLanguage is in the supported list and a template exists → Available
     * - If preferredLanguage is not in the supported list → fall back to topicSourceLanguage, return Available
     * - If preferredLanguage is in the supported list but template not yet generated
     *   → trigger generation, return Pending
     *
     * Gating logic lives here, not in the controller.
     */
    fun resolveTemplate(
        preferredLanguage: String,
        topicSourceLanguage: String,
        moduleId: String,
        dayIndex: Int
    ): LanguageGatingResult {
        val effectiveLanguage =
            if (preferredLanguage in properties.supportedLanguages) preferredLanguage
            else topicSourceLanguage

        val template = taskTemplateRepository.findCurrentByModuleIdAndDayIndexAndLanguage(
            moduleId, dayIndex, effectiveLanguage
        )

        return if (template != null) {
            LanguageGatingResult.Available(template)
        } else {
            generationPort.triggerGeneration(moduleId, dayIndex, effectiveLanguage)
            LanguageGatingResult.Pending(effectiveLanguage)
        }
    }
}
