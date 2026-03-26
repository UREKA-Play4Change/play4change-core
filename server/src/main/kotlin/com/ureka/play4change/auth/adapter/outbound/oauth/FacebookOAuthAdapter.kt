package com.ureka.play4change.auth.adapter.outbound.oauth

import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.OAuthClaims
import com.ureka.play4change.auth.port.outbound.OAuthVerifierPort
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component("facebookOAuthAdapter")
class FacebookOAuthAdapter : OAuthVerifierPort {

    private val restTemplate = RestTemplate()

    override fun verify(provider: AuthProvider, idToken: String): OAuthClaims {
        require(provider == AuthProvider.FACEBOOK) { "FacebookOAuthAdapter only handles FACEBOOK" }
        val url = "https://graph.facebook.com/me?fields=id,name,email&access_token=$idToken"
        val response = try {
            restTemplate.getForEntity(url, Map::class.java)
        } catch (ex: Exception) {
            throw IllegalArgumentException("Facebook token verification failed: ${ex.message}")
        }
        if (!response.statusCode.is2xxSuccessful) {
            throw IllegalArgumentException("Facebook rejected token: ${response.statusCode}")
        }
        val body = response.body
            ?: throw IllegalArgumentException("Empty response from Facebook")
        return OAuthClaims(
            email = body["email"]?.toString()
                ?: throw IllegalArgumentException("No email from Facebook"),
            name = body["name"]?.toString(),
            sub = body["id"]?.toString()
                ?: throw IllegalArgumentException("No id from Facebook")
        )
    }
}
