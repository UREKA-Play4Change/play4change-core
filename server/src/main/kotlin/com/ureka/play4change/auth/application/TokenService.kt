package com.ureka.play4change.auth.application

import com.ureka.play4change.auth.domain.crypto.AuthCrypto
import com.ureka.play4change.auth.domain.model.RefreshToken
import com.ureka.play4change.auth.domain.model.TokenPair
import com.ureka.play4change.auth.port.inbound.TokenUseCase
import com.ureka.play4change.auth.port.outbound.RefreshTokenRepository
import com.ureka.play4change.auth.port.outbound.UserRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID

data class AccessTokenClaims(val userId: String, val role: String)

private val VALID_ROLES = setOf("USER", "ADMIN")

@Service
class TokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    // Needed to fetch email for rotated access tokens; keeps refresh_tokens narrow (no email column)
    private val userRepository: UserRepository,
    private val jwtProperties: JwtProperties,
    private val clock: Clock
) : TokenUseCase {

    private val log = LoggerFactory.getLogger(TokenService::class.java)

    private val signingKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    /** Called by MagicLinkService after user is resolved. */
    fun issue(userId: String, email: String, role: String): TokenPair {
        val accessExpirySeconds = jwtProperties.accessTtlMinutes * 60
        val accessToken = buildAccessToken(userId, email, role, accessExpirySeconds)
        val rawRefresh = AuthCrypto.generateOpaqueToken()
        val familyId = UUID.randomUUID().toString()

        refreshTokenRepository.save(
            RefreshToken(
                id = UUID.randomUUID().toString(),
                tokenHash = AuthCrypto.sha256Hex(rawRefresh),
                userId = userId,
                familyId = familyId,
                expiresAt = OffsetDateTime.now(clock).plusDays(jwtProperties.refreshTtlDays),
                used = false,
                createdAt = OffsetDateTime.now(clock),
                role = role
            )
        )
        return TokenPair(accessToken, rawRefresh, accessExpirySeconds)
    }

    override fun refresh(rawRefreshToken: String): TokenPair {
        val stored = refreshTokenRepository.findByTokenHash(AuthCrypto.sha256Hex(rawRefreshToken))
            ?: throw IllegalArgumentException("Refresh token not found")

        if (stored.used) {
            // Token reuse = theft. Revoke entire family. Force re-auth.
            refreshTokenRepository.revokeAllByFamilyId(stored.familyId)
            log.warn(
                "SECURITY: refresh token reuse detected for userId={}, familyId={} — all sessions revoked",
                stored.userId, stored.familyId
            )
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
        val accessToken = buildAccessToken(stored.userId, user.email, stored.role, accessExpirySeconds)
        val rawRefresh = AuthCrypto.generateOpaqueToken()

        refreshTokenRepository.save(
            RefreshToken(
                id = UUID.randomUUID().toString(),
                tokenHash = AuthCrypto.sha256Hex(rawRefresh),
                userId = stored.userId,
                familyId = stored.familyId,
                expiresAt = OffsetDateTime.now(clock).plusDays(jwtProperties.refreshTtlDays),
                used = false,
                createdAt = OffsetDateTime.now(clock),
                role = stored.role
            )
        )
        return TokenPair(accessToken, rawRefresh, accessExpirySeconds)
    }

    override fun revoke(rawRefreshToken: String): String? {
        val stored = refreshTokenRepository.findByTokenHash(AuthCrypto.sha256Hex(rawRefreshToken))
            ?: return null
        // Revoke all tokens in the family so stolen tokens from the same session are also invalidated
        refreshTokenRepository.revokeAllByFamilyId(stored.familyId)
        return stored.userId
    }

    fun parseAccessToken(token: String): AccessTokenClaims {
        val payload = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
        val role = payload.get("role", String::class.java)?.takeIf { it in VALID_ROLES } ?: "USER"
        return AccessTokenClaims(
            userId = payload.subject,
            role = role
        )
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Builds a signed JWT access token. Single point of change for claims structure,
     * algorithm, or expiry strategy.
     */
    private fun buildAccessToken(
        userId: String,
        email: String,
        role: String,
        expirySeconds: Long
    ): String = Jwts.builder()
        .subject(userId)
        .claim("email", email)
        .claim("role", role)
        .issuedAt(Date.from(clock.instant()))
        .expiration(Date.from(clock.instant().plusSeconds(expirySeconds)))
        .signWith(signingKey)
        .compact()
}
