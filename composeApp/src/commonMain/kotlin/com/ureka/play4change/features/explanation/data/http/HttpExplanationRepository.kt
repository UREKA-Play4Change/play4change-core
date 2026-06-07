package com.ureka.play4change.features.explanation.data.http

import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.networkErrorFromStatus
import com.ureka.play4change.features.explanation.domain.model.ExplanationMessage
import com.ureka.play4change.features.explanation.domain.model.ExplanationSession
import com.ureka.play4change.features.explanation.domain.repository.ExplanationRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// Network DTOs
// ---------------------------------------------------------------------------

@Serializable
private data class ExplanationSessionDto(
    val sessionId: String,
    val status: String,
    val explanationText: String? = null,
    val messages: List<ExplanationMessageDto> = emptyList()
)

@Serializable
private data class ExplanationMessageDto(
    val id: String,
    val role: String,
    val content: String
)

@Serializable
private data class SendMessageRequestDto(val content: String)

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

class HttpExplanationRepository(private val client: HttpClient) : ExplanationRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getSession(sessionId: String): ExplanationSession {
        val response = client.get("explanation/$sessionId")
        if (!response.status.isSuccess()) {
            throw NetworkException(networkErrorFromStatus(response.status.value))
        }
        return json.decodeFromString<ExplanationSessionDto>(response.bodyAsText()).toDomain()
    }

    override suspend fun sendMessage(sessionId: String, content: String): ExplanationMessage {
        val response = client.post("explanation/$sessionId/message") {
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequestDto(content))
        }
        if (!response.status.isSuccess()) {
            throw NetworkException(networkErrorFromStatus(response.status.value))
        }
        return json.decodeFromString<ExplanationMessageDto>(response.bodyAsText()).toDomain()
    }

    override suspend fun resolve(sessionId: String) {
        val response = client.post("explanation/$sessionId/resolve")
        if (!response.status.isSuccess()) {
            throw NetworkException(networkErrorFromStatus(response.status.value))
        }
    }

    private fun ExplanationSessionDto.toDomain() = ExplanationSession(
        sessionId = sessionId,
        status = status,
        explanationText = explanationText,
        messages = messages.map { it.toDomain() }
    )

    private fun ExplanationMessageDto.toDomain() = ExplanationMessage(
        id = id,
        role = role,
        content = content
    )
}
