package com.ureka.play4change.infrastructure.language

import com.ureka.play4change.application.port.LanguageGenerationPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Stub implementation of [LanguageGenerationPort].
 * Real multi-language Mistral generation is wired in Task 2.4.
 * See HACKS.md H05.
 */
@Component
class NoOpLanguageGenerationAdapter : LanguageGenerationPort {

    private val log = LoggerFactory.getLogger(NoOpLanguageGenerationAdapter::class.java)

    override fun triggerGeneration(moduleId: String, dayIndex: Int, language: String) {
        log.info(
            "Language generation queued (stub): moduleId={} dayIndex={} language={}" +
                " — Task 2.4 implements real generation",
            moduleId, dayIndex, language
        )
    }
}
