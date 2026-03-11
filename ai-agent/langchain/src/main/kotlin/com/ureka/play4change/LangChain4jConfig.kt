package com.ureka.play4change.config

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.mistralai.MistralAiChatModel
import dev.langchain4j.model.mistralai.MistralAiChatModelName
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

// ─────────────────────────────────────────────────────────────────────────────
//  LangChain4jConfig
//
//  Wires LangChain4j beans. This is the ONLY place Mistral is configured.
//  To swap providers: replace these beans with OpenAI/Gemini equivalents.
//  Everything else in :ai-agent:langchain and :server remains unchanged.
//
//  Provider agnosticism is achieved through:
//  1. TaskGenerationPort interface (:ai-agent:api)
//  2. LangChain4jTaskGenerationAdapter implements it (:ai-agent:langchain)
//  3. ChatLanguageModel and EmbeddingModel are LangChain4j abstractions
//     — they work the same regardless of which provider backs them
// ─────────────────────────────────────────────────────────────────────────────
@Configuration
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
            .logRequests(false)   // set true for debugging, never in prod
            .logResponses(false)
            .build()
    }

    @Bean
    fun embeddingModel(): EmbeddingModel {
        // Mistral embedding model for pgvector similarity search
        // Same API key, different endpoint
        return MistralAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .build()
    }
}