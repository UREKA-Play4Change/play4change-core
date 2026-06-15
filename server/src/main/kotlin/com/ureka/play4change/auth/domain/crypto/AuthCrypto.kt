package com.ureka.play4change.auth.domain.crypto

import org.springframework.security.crypto.codec.Hex
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Cryptographic helpers shared across the auth bounded context.
 *
 * Both [com.ureka.play4change.auth.application.MagicLinkService] and
 * [com.ureka.play4change.auth.application.TokenService] need token generation
 * and SHA-256 hashing. Centralising here ensures a single place to upgrade
 * the algorithm (e.g. switch to BLAKE3, add a pepper) without divergence.
 */
object AuthCrypto {

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure 32-byte random token, hex-encoded (64 chars).
     * Only the SHA-256 hash of this token should ever be persisted.
     */
    fun generateOpaqueToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return String(Hex.encode(bytes))
    }

    /**
     * Returns the hex-encoded SHA-256 digest of [input] encoded as UTF-8.
     */
    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return String(Hex.encode(digest.digest(input.toByteArray(Charsets.UTF_8))))
    }
}
