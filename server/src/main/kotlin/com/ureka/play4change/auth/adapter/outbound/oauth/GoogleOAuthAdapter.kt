package com.ureka.play4change.auth.adapter.outbound.oauth

import com.fasterxml.jackson.databind.ObjectMapper
import com.ureka.play4change.auth.application.GoogleProperties
import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.OAuthClaims
import com.ureka.play4change.auth.port.outbound.OAuthVerifierPort
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

@Component("googleOAuthAdapter")
class GoogleOAuthAdapter(
    private val googleProperties: GoogleProperties,
    private val objectMapper: ObjectMapper
) : OAuthVerifierPort {

    private val restTemplate = RestTemplate()

    @Volatile private var jwksCache: Pair<Long, String>? = null
    private val jwksCacheTtlMs = 3_600_000L  // 1 hour

    companion object {
        private const val JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs"
        private val VALID_ISSUERS = setOf("https://accounts.google.com", "accounts.google.com")
    }

    override fun verify(provider: AuthProvider, idToken: String): OAuthClaims {
        require(provider == AuthProvider.GOOGLE) { "GoogleOAuthAdapter only handles GOOGLE" }

        val kid = extractKid(idToken)
        val publicKey = findPublicKey(kid)

        val claims = try {
            Jwts.parser()
                .keyLocator { _ -> publicKey }
                .build()
                .parseSignedClaims(idToken)
                .payload
        } catch (ex: Exception) {
            throw IllegalArgumentException("Google ID token signature verification failed: ${ex.message}")
        }

        require(claims.issuer in VALID_ISSUERS) {
            "Invalid Google token issuer: ${claims.issuer}"
        }

        if (googleProperties.clientId.isNotBlank()) {
            require(claims.audience.contains(googleProperties.clientId)) {
                "Google token audience does not match configured client ID"
            }
        }

        // JJWT validates exp automatically; reaching here means token is not expired

        return OAuthClaims(
            email = claims.get("email", String::class.java)
                ?: throw IllegalArgumentException("No email claim in Google ID token"),
            name = claims.get("name", String::class.java),
            sub = claims.subject
                ?: throw IllegalArgumentException("No sub claim in Google ID token")
        )
    }

    /** Extract the `kid` from the unverified JWT header (base64url-encoded JSON). */
    private fun extractKid(token: String): String {
        val headerPart = token.substringBefore(".")
        val headerJson = String(Base64.getUrlDecoder().decode(headerPart))
        return Regex(""""kid"\s*:\s*"([^"]+)"""")
            .find(headerJson)
            ?.groupValues?.get(1)
            ?: throw IllegalArgumentException("No kid in Google JWT header")
    }

    /** Fetch JWKS (cached for 1 h), find the key matching kid, build RSA PublicKey. */
    private fun findPublicKey(kid: String): PublicKey {
        val jwksJson = fetchJwks()
        val keysNode = objectMapper.readTree(jwksJson).get("keys")
            ?: throw IllegalStateException("Invalid JWKS response from Google: no 'keys' array")

        val keyNode = keysNode.find { it.get("kid")?.asText() == kid }
            ?: throw IllegalArgumentException("No key found in Google JWKS for kid=$kid")

        val n = keyNode.get("n")?.asText()
            ?: throw IllegalStateException("JWK missing 'n' (modulus)")
        val e = keyNode.get("e")?.asText()
            ?: throw IllegalStateException("JWK missing 'e' (exponent)")

        return buildRsaPublicKey(n, e)
    }

    private fun fetchJwks(): String {
        val cached = jwksCache
        if (cached != null && System.currentTimeMillis() - cached.first < jwksCacheTtlMs) {
            return cached.second
        }
        val json = restTemplate.getForObject(JWKS_URL, String::class.java)
            ?: throw IllegalStateException("Failed to fetch Google JWKS from $JWKS_URL")
        jwksCache = System.currentTimeMillis() to json
        return json
    }

    private fun buildRsaPublicKey(n: String, e: String): PublicKey {
        val modulus = BigInteger(1, Base64.getUrlDecoder().decode(n))
        val exponent = BigInteger(1, Base64.getUrlDecoder().decode(e))
        return KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(modulus, exponent))
    }
}
