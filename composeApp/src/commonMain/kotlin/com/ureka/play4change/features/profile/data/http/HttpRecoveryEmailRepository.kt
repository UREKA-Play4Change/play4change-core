package com.ureka.play4change.features.profile.data.http

import com.ureka.play4change.core.network.NetworkError
import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.networkErrorFromStatus
import com.ureka.play4change.features.profile.domain.model.RecoveryEmail
import com.ureka.play4change.features.profile.domain.repository.RecoveryEmailRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RecoveryEmailDto(val id: String, val email: String, val verified: Boolean)

@Serializable
private data class AddRecoveryEmailBody(val email: String)

@Serializable
private data class VerifyTokenBody(val token: String)

@Serializable
private data class ServerMessageBody(val message: String)

class HttpRecoveryEmailRepository(private val client: HttpClient) : RecoveryEmailRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listRecoveryEmails(): List<RecoveryEmail> {
        val response = client.get("account/recovery-emails")
        if (!response.status.isSuccess()) throw NetworkException(networkErrorFromStatus(response.status.value))
        return json.decodeFromString<List<RecoveryEmailDto>>(response.bodyAsText())
            .map { RecoveryEmail(it.id, it.email, it.verified) }
    }

    override suspend fun addRecoveryEmail(email: String) {
        val response = client.post("account/recovery-emails") {
            contentType(ContentType.Application.Json)
            setBody(AddRecoveryEmailBody(email))
        }
        if (!response.status.isSuccess()) {
            if (response.status.value == 400) {
                val serverMessage = runCatching {
                    json.decodeFromString<ServerMessageBody>(response.bodyAsText()).message
                }.getOrNull() ?: "Invalid request"
                throw NetworkException(NetworkError.Unknown(serverMessage))
            }
            throw NetworkException(networkErrorFromStatus(response.status.value))
        }
    }

    override suspend fun removeRecoveryEmail(id: String) {
        val response = client.delete("account/recovery-emails/$id")
        if (!response.status.isSuccess()) throw NetworkException(networkErrorFromStatus(response.status.value))
    }

    override suspend fun verifyRecoveryEmail(token: String) {
        val response = client.post("auth/recovery-email/verify") {
            contentType(ContentType.Application.Json)
            setBody(VerifyTokenBody(token))
        }
        if (!response.status.isSuccess()) {
            if (response.status.value == 400) {
                val serverMessage = runCatching {
                    json.decodeFromString<ServerMessageBody>(response.bodyAsText()).message
                }.getOrNull() ?: "Invalid or expired verification code"
                throw NetworkException(NetworkError.Unknown(serverMessage))
            }
            throw NetworkException(networkErrorFromStatus(response.status.value))
        }
    }
}