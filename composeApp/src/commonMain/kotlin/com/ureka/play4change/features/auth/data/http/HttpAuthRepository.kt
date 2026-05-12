@file:OptIn(ExperimentalEncodingApi::class)

package com.ureka.play4change.features.auth.data.http

import com.ureka.play4change.auth.AuthProvider
import com.ureka.play4change.auth.AuthTokens
import com.ureka.play4change.core.network.NetworkError
import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.TokenStorage
import com.ureka.play4change.features.auth.domain.model.AuthResult
import com.ureka.play4change.features.auth.domain.model.MagicLinkResult
import com.ureka.play4change.features.auth.domain.model.SocialProvider
import com.ureka.play4change.features.auth.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// ---------------------------------------------------------------------------
// Network DTOs
// ---------------------------------------------------------------------------

@Serializable
private data class MagicLinkRequestBody(val email: String)

@Serializable
private data class MagicLinkVerifyBody(val token: String)

@Serializable
private data class OAuthRequestBody(val provider: AuthProvider, val idToken: String)

@Serializable
private data class RefreshRequestBody(val refreshToken: String)

@Serializable
private data class TokenResponseBody(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

class HttpAuthRepository(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    override suspend fun sendMagicLink(email: String): MagicLinkResult {
        val response = client.post("/auth/magic-link") {
            contentType(ContentType.Application.Json)
            setBody(MagicLinkRequestBody(email))
        }
        return MagicLinkResult(success = response.status.isSuccess())
    }

    override suspend fun verifyMagicLink(token: String): AuthResult? {
        val response = client.post("/auth/magic-link/verify") {
            contentType(ContentType.Application.Json)
            setBody(MagicLinkVerifyBody(token))
        }
        return when {
            response.status.isSuccess() -> {
                val body = Json.decodeFromString<TokenResponseBody>(response.bodyAsText())
                tokenStorage.store(body.accessToken, body.refreshToken)
                body.toAuthResult()
            }
            response.status == HttpStatusCode.Unauthorized ->
                throw NetworkException(NetworkError.Unauthorized)
            response.status == HttpStatusCode.Forbidden ->
                throw NetworkException(NetworkError.Forbidden)
            response.status.value in 500..599 ->
                throw NetworkException(NetworkError.ServerError(response.status.value))
            else ->
                throw NetworkException(NetworkError.Unknown("HTTP ${response.status.value}"))
        }
    }

    override suspend fun socialLogin(provider: SocialProvider, idToken: String): AuthResult? {
        if (idToken.isBlank()) return null
        val response = client.post("/auth/oauth") {
            contentType(ContentType.Application.Json)
            setBody(OAuthRequestBody(provider.toAuthProvider(), idToken))
        }
        if (!response.status.isSuccess()) return null
        val body = Json.decodeFromString<TokenResponseBody>(response.bodyAsText())
        tokenStorage.store(body.accessToken, body.refreshToken)
        return body.toAuthResult()
    }

    override suspend fun refresh(refreshToken: String): AuthResult? {
        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequestBody(refreshToken))
        }
        if (!response.status.isSuccess()) return null
        val body = Json.decodeFromString<TokenResponseBody>(response.bodyAsText())
        tokenStorage.store(body.accessToken, body.refreshToken)
        return body.toAuthResult()
    }

    override suspend fun register(name: String, email: String): Boolean {
        // Registration on this server is handled by the magic link flow.
        // Sending a magic link also creates the account on first use.
        sendMagicLink(email)
        return true
    }

    override suspend fun logout() {
        val refreshToken = tokenStorage.getRefreshToken()
        tokenStorage.clear()
        if (refreshToken != null) {
            runCatching {
                client.post("/auth/logout") {
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequestBody(refreshToken))
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun TokenResponseBody.toAuthResult(): AuthResult = AuthResult(
        userId = extractSubFromJwt(accessToken),
        tokens = AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
        )
    )

    /**
     * Extracts the `sub` claim from the JWT payload without verifying the signature.
     * Signature verification is the server's responsibility; the client only needs
     * the user identifier to route to profile endpoints.
     */
    private fun extractSubFromJwt(jwt: String): String = try {
        val payload = jwt.split(".").getOrElse(1) { return "" }
        val decoded = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(payload)
        val json = Json.parseToJsonElement(decoded.decodeToString()).jsonObject
        json["sub"]?.jsonPrimitive?.content ?: ""
    } catch (_: Exception) {
        ""
    }

    private fun SocialProvider.toAuthProvider(): AuthProvider = when (this) {
        SocialProvider.GOOGLE -> AuthProvider.GOOGLE
        SocialProvider.FACEBOOK -> AuthProvider.FACEBOOK
    }
}
