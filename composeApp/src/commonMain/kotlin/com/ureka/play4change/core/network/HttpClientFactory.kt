@file:OptIn(ExperimentalUuidApi::class)

package com.ureka.play4change.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
private data class RefreshBody(val refreshToken: String)

@Serializable
private data class TokensBody(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

object HttpClientFactory {

    /**
     * Creates the application [HttpClient].
     *
     * @param tokenStorage      Secure token storage for the current platform.
     * @param networkConfig     Contains the base URL for all requests.
     * @param onSessionExpired  Called when refresh fails — clears storage and notifies callers.
     * @param engine            Optional engine override used by unit tests (MockEngine).
     *                          When null the platform engine (OkHttp / Darwin) is used.
     */
    fun create(
        tokenStorage: TokenStorage,
        networkConfig: NetworkConfig,
        onSessionExpired: () -> Unit,
        engine: HttpClientEngine? = null,
    ): HttpClient = if (engine != null) {
        HttpClient(engine) {
            applyConfig(tokenStorage, networkConfig, onSessionExpired)
        }
    } else {
        platformHttpClient {
            applyConfig(tokenStorage, networkConfig, onSessionExpired)
        }
    }

    private fun HttpClientConfig<*>.applyConfig(
        tokenStorage: TokenStorage,
        networkConfig: NetworkConfig,
        onSessionExpired: () -> Unit,
    ) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val access = tokenStorage.getAccessToken() ?: return@loadTokens null
                    val refresh = tokenStorage.getRefreshToken() ?: return@loadTokens null
                    BearerTokens(access, refresh)
                }
                refreshTokens {
                    val refreshToken = oldTokens?.refreshToken ?: tokenStorage.getRefreshToken()
                    if (refreshToken == null) {
                        tokenStorage.clear()
                        onSessionExpired()
                        return@refreshTokens null
                    }

                    try {
                        val response = client.post("${networkConfig.baseUrl}/auth/refresh") {
                            markAsRefreshTokenRequest()
                            contentType(ContentType.Application.Json)
                            setBody(RefreshBody(refreshToken))
                        }

                        if (response.status.isSuccess()) {
                            val tokens = Json.decodeFromString<TokensBody>(response.bodyAsText())
                            tokenStorage.store(tokens.accessToken, tokens.refreshToken)
                            BearerTokens(tokens.accessToken, tokens.refreshToken)
                        } else {
                            tokenStorage.clear()
                            onSessionExpired()
                            null
                        }
                    } catch (_: Exception) {
                        tokenStorage.clear()
                        onSessionExpired()
                        null
                    }
                }
                sendWithoutRequest { true }
            }
        }

        defaultRequest {
            url(networkConfig.baseUrl)
            header("X-Request-ID", Uuid.random().toString())
        }
    }
}
