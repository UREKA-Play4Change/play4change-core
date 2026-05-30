package com.ureka.play4change.auth.adapter.inbound.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.TimeMeter
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Service
class RateLimitService(val timeMeter: TimeMeter) {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun tryConsume(clientIp: String, path: String): Boolean {
        val capacity = capacityFor(path) ?: return true
        val key = "$clientIp:${normalizePath(path)}"
        val bucket = buckets.computeIfAbsent(key) { buildBucket(capacity) }
        return bucket.tryConsume(1)
    }

    fun retryAfterSeconds(clientIp: String, path: String): Long {
        val key = "$clientIp:${normalizePath(path)}"
        val bucket = buckets[key] ?: return 0L
        val probe = bucket.estimateAbilityToConsume(1)
        return (probe.nanosToWaitForRefill / 1_000_000_000L).coerceAtLeast(1L)
    }

    fun clear() = buckets.clear()

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

    private fun capacityFor(path: String): Int? = when (normalizePath(path)) {
        "/auth/magic-link" -> 5
        "/auth/magic-link/verify" -> 10
        "/auth/verify" -> 10
        "/auth/oauth" -> 10
        "/auth/refresh" -> 20
        "/auth/logout" -> 10
        else -> null
    }

    private fun normalizePath(path: String): String =
        path.trimEnd('/').lowercase()
}
