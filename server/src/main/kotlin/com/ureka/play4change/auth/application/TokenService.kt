package com.ureka.play4change.auth.application

import com.ureka.play4change.auth.domain.model.RefreshToken
import com.ureka.play4change.auth.domain.model.TokenPair
import com.ureka.play4change.auth.port.inbound.TokenUseCase
import com.ureka.play4change.auth.port.outbound.RefreshTokenRepository
import com.ureka.play4change.auth.port.outbound.UserRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.crypto.codec.Hex
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID

data class AccessTokenClaims(val userId: String, val role: String)

@Service
class TokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    // Needed to fetch email for rotated access tokens; keeps refresh_tokens narrow (no email column)
    private val userRepository: UserRepository,
    private val jwtProperties: JwtProperties
) : TokenUseCase {

    private val signingKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    /** Called by MagicLinkService and OAuthService after user is resolved. */
    fun issue(userId: String, email: String, role: String): TokenPair {
        val accessExpirySeconds = jwtProperties.accessTtlMinutes * 60
        val accessToken = Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("role", role)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessExpirySeconds * 1000L))
            .signWith(signingKey)
            .compact()

        val rawRefresh = generateSecureToken()
        val hash = sha256(rawRefresh)
        val familyId = UUID.randomUUID().toString()

        refreshTokenRepository.save(
            RefreshToken(
                id = UUID.randomUUID().toString(),
                tokenHash = hash,
                userId = userId,
                familyId = familyId,
                expiresAt = OffsetDateTime.now().plusDays(jwtProperties.refreshTtlDays),
                used = false,
                createdAt = OffsetDateTime.now(),
                role = role
            )
        )
        return TokenPair(accessToken, rawRefresh, accessExpirySeconds)
    }

    override fun refresh(rawRefreshToken: String): TokenPair {
        val hash = sha256(rawRefreshToken)
        val stored = refreshTokenRepository.findByTokenHash(hash)
            ?: throw IllegalArgumentException("Refresh token not found")

        if (stored.used) {
            // Token reuse = theft. Revoke entire family. Force re-auth.
            refreshTokenRepository.revokeAllByFamilyId(stored.familyId)
            throw SecurityException("Refresh token reuse detected. All sessions revoked.")
        }

        if (!stored.isValid()) {
            throw IllegalArgumentException("Refresh token expired")
        }

        refreshTokenRepository.markUsed(stored.id)

        // Load user by primary key (one indexed lookup) to include email in the rotated access token
        val user = userRepository.findById(stored.userId)
            ?: throw IllegalArgumentException("User not found")

        val accessExpirySeconds = jwtProperties.accessTtlMinutes * 60
        val accessToken = Jwts.builder()
            .subject(stored.userId)
            .claim("email", user.email)
            .claim("role", stored.role)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessExpirySeconds * 1000L))
            .signWith(signingKey)
            .compact()

        val rawRefresh = generateSecureToken()
        val newHash = sha256(rawRefresh)

        refreshTokenRepository.save(
            RefreshToken(
                id = UUID.randomUUID().toString(),
                tokenHash = newHash,
                userId = stored.userId,
                familyId = stored.familyId,
                expiresAt = OffsetDateTime.now().plusDays(jwtProperties.refreshTtlDays),
                used = false,
                createdAt = OffsetDateTime.now(),
                role = stored.role
            )
        )
        return TokenPair(accessToken, rawRefresh, accessExpirySeconds)
    }

    override fun revoke(rawRefreshToken: String) {
        val hash = sha256(rawRefreshToken)
        val stored = refreshTokenRepository.findByTokenHash(hash) ?: return
        // Revoke all tokens in the family so stolen tokens from the same session are also invalidated
        refreshTokenRepository.revokeAllByFamilyId(stored.familyId)
    }

    fun parseAccessToken(token: String): AccessTokenClaims {
        val payload = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return AccessTokenClaims(
            userId = payload.subject,
            role = payload.get("role", String::class.java) ?: "USER"
        )
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return String(Hex.encode(bytes))
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return String(Hex.encode(digest.digest(input.toByteArray())))
    }
}
