package com.ureka.play4change.auth.adapter.inbound.security

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.TimeMeter
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * In-process rate limiter backed by Bucket4j.
 *
 * Buckets are stored in a Caffeine cache with a 15-minute access-based expiry and a
 * size cap of 50 000 entries. This prevents the unbounded memory growth that a plain
 * [java.util.concurrent.ConcurrentHashMap] would exhibit under high-cardinality IP traffic.
 *
 * Switch to `bucket4j-redis` (or `bucket4j-hazelcast`) when horizontal scaling is needed.
 */
@Service
class RateLimitService(private val timeMeter: TimeMeter) {

    private val buckets = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(15))
        .maximumSize(50_000L)
        .build<String, Bucket>()

    fun tryConsume(clientIp: String, path: String): Boolean {
        val capacity = capacityFor(path) ?: return true
        val key = "$clientIp:${normalizePath(path)}"
        val bucket = buckets.get(key) { buildBucket(capacity) }
        return bucket.tryConsume(1)
    }

    fun retryAfterSeconds(clientIp: String, path: String): Long {
        val key = "$clientIp:${normalizePath(path)}"
        val bucket = buckets.getIfPresent(key) ?: return 0L
        val probe = bucket.estimateAbilityToConsume(1)
        return (probe.nanosToWaitForRefill / 1_000_000_000L).coerceAtLeast(1L)
    }

    /** Only used in tests — clears all cached buckets. */
    fun clear() = buckets.invalidateAll()

    private fun buildBucket(capacity: Int): Bucket =
        Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(capacity.toLong())
                    .refillIntervally(capacity.toLong(), Duration.ofMinutes(10))
                    .build()
            )
            .withCustomTimePrecision(timeMeter)
            .build()

    private fun capacityFor(path: String): Int? {
        val normalized = normalizePath(path)
        return when {
            normalized == "/auth/magic-link" -> 5
            normalized == "/auth/magic-link/verify" -> 10
            normalized == "/auth/verify" -> 10
            normalized == "/auth/oauth" -> 10
            normalized == "/auth/refresh" -> 20
            normalized == "/auth/logout" -> 10
            normalized == "/auth/recovery-email/verify" -> 10
            // Per-IP caps on high-frequency API paths — prevents brute-force answer submission,
            // verdict flooding, and struggle path replay attacks.
            normalized.matches(Regex("/topics/[^/]+/task-assignments/[^/]+/submit")) -> 30
            normalized.matches(Regex("/topics/[^/]+/task-assignments/[^/]+/photo")) -> 20
            normalized.matches(Regex("/reviews/[^/]+/verdict")) -> 20
            normalized.matches(Regex("/enrollments/[^/]+/struggle/[^/]+/submit")) -> 30
            else -> null
        }
    }

    private fun normalizePath(path: String): String =
        path.trimEnd('/').lowercase()
}
