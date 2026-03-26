package com.ureka.play4change.auth.adapter.outbound.oauth

import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.OAuthClaims
import com.ureka.play4change.auth.port.outbound.OAuthVerifierPort
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component("googleOAuthAdapter")
class GoogleOAuthAdapter : OAuthVerifierPort {

    private val restTemplate = RestTemplate()

    override fun verify(provider: AuthProvider, idToken: String): OAuthClaims {
        require(provider == AuthProvider.GOOGLE) { "GoogleOAuthAdapter only handles GOOGLE" }
        val url = "https://oauth2.googleapis.com/tokeninfo?id_token=$idToken"
        val response = try {
            restTemplate.getForEntity(url, Map::class.java)
        } catch (ex: Exception) {
            throw IllegalArgumentException("Google token verification failed: ${ex.message}")
        }
        if (!response.statusCode.is2xxSuccessful) {
            throw IllegalArgumentException("Google rejected ID token: ${response.statusCode}")
        }
        val body = response.body
            ?: throw IllegalArgumentException("Empty response from Google")
        return OAuthClaims(
            email = body["email"]?.toString()
                ?: throw IllegalArgumentException("No email in Google token"),
            name = body["name"]?.toString(),
            sub = body["sub"]?.toString()
                ?: throw IllegalArgumentException("No sub in Google token")
        )
    }
}
