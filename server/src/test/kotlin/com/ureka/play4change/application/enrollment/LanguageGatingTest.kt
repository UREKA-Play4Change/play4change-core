package com.ureka.play4change.application.enrollment

import com.ureka.play4change.application.port.LanguageGenerationPort
import com.ureka.play4change.config.LanguageProperties
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class LanguageGatingTest {

    private val properties = LanguageProperties().apply {
        supportedLanguages = listOf("en", "pt-PT", "es-ES")
    }
    private val taskTemplateRepository = mockk<TaskTemplateRepository>()
    private val generationPort = mockk<LanguageGenerationPort>(relaxed = true)

    private val service = LanguageGatingService(properties, taskTemplateRepository, generationPort)

    private fun makeTemplate(language: String) = TaskTemplate(
        id = "tmpl-$language",
        moduleId = "module-1",
        dayIndex = 0,
        poolIndex = 0,
        title = "Task in $language",
        description = "Description",
        hint = null,
        taskType = TaskType.MULTIPLE_CHOICE,
        pointsReward = 20,
        options = listOf("A", "B", "C"),
        correctAnswer = 0,
        version = 1,
        isCurrent = true,
        supersededBy = null,
        embedding = null,
        language = language,
        createdAt = OffsetDateTime.now()
    )

    @Test
    fun `given existing language variant when resolving template then returns available instance`() {
        val ptTemplate = makeTemplate("pt-PT")
        every {
            taskTemplateRepository.findCurrentByModuleIdAndDayIndexAndLanguage("module-1", 0, "pt-PT")
        } returns ptTemplate

        val result = service.resolveTemplate(
            preferredLanguage = "pt-PT",
            topicSourceLanguage = "en",
            moduleId = "module-1",
            dayIndex = 0
        )

        assertTrue(result is LanguageGatingResult.Available)
        assertEquals(ptTemplate, (result as LanguageGatingResult.Available).template)
    }

    @Test
    fun `given unsupported language when resolving template then falls back to topic source language`() {
        val enTemplate = makeTemplate("en")
        every {
            taskTemplateRepository.findCurrentByModuleIdAndDayIndexAndLanguage("module-1", 0, "en")
        } returns enTemplate

        val result = service.resolveTemplate(
            preferredLanguage = "fr-FR",
            topicSourceLanguage = "en",
            moduleId = "module-1",
            dayIndex = 0
        )

        assertTrue(result is LanguageGatingResult.Available)
        assertEquals(enTemplate, (result as LanguageGatingResult.Available).template)
    }

    @Test
    fun `given supported language not yet generated when resolving then triggers generation and returns pending`() {
        every {
            taskTemplateRepository.findCurrentByModuleIdAndDayIndexAndLanguage("module-1", 0, "pt-PT")
        } returns null

        val result = service.resolveTemplate(
            preferredLanguage = "pt-PT",
            topicSourceLanguage = "en",
            moduleId = "module-1",
            dayIndex = 0
        )

        assertTrue(result is LanguageGatingResult.Pending)
        assertEquals("pt-PT", (result as LanguageGatingResult.Pending).requestedLanguage)
        verify(exactly = 1) { generationPort.triggerGeneration("module-1", 0, "pt-PT") }
    }
}
