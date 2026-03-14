package com.ureka.play4change.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.mistralai.MistralAiChatModel
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

// ─────────────────────────────────────────────────────────────────────────────
//  LangChain4jConfig
//
//  @ConditionalOnProperty means these beans only load when
//  ai.mistral.api-key is set to a real value in application.yml / .env
//
//  Without a key: Spring starts fine, AI endpoints return 503 gracefully
//  With a key: full AI pipeline activates automatically
//
//  This is the correct pattern for optional external dependencies.
// ─────────────────────────────────────────────────────────────────────────────
@Configuration
@ConditionalOnProperty(name = ["ai.mistral.api-key"], matchIfMissing = false)
class LangChain4jConfig(
    @Value("\${ai.mistral.api-key}") private val apiKey: String,
    @Value("\${ai.mistral.model:mistral-small-latest}") private val model: String,
    @Value("\${ai.mistral.temperature:0.7}") private val temperature: Double,
    @Value("\${ai.mistral.timeout-seconds:60}") private val timeoutSeconds: Long,
) {

    @Bean
    fun chatLanguageModel(): ChatLanguageModel {
        return MistralAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(model)
            .temperature(temperature)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .logRequests(false)
            .logResponses(false)
            .build()
    }

    @Bean
    fun embeddingModel(): EmbeddingModel {
        return MistralAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .build()
    }
}