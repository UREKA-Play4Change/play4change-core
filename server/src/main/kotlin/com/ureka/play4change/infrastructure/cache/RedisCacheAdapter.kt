package com.ureka.play4change.infrastructure.cache

import com.ureka.play4change.application.port.CachePort
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisCacheAdapter(
    private val redis: RedisTemplate<String, String>,
    private val registry: MeterRegistry
) : CachePort {

    private val log = LoggerFactory.getLogger(RedisCacheAdapter::class.java)

    override fun get(key: String): String? {
        val keyPrefix = key.substringBefore(":", missingDelimiterValue = key)
        registry.counter("cache_requests_total", "key_prefix", keyPrefix).increment()
        return try {
            val result = redis.opsForValue().get(key)
            if (result != null) {
                registry.counter("cache_hits_total", "key_prefix", keyPrefix).increment()
            }
            result
        } catch (ex: Exception) {
            log.warn("Redis GET failed for key '{}': {}", key, ex.message)
            null
        }
    }

    override fun put(key: String, value: String, ttlSeconds: Long) {
        try {
            redis.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds))
        } catch (ex: Exception) {
            log.warn("Redis PUT failed for key '{}': {}", key, ex.message)
        }
    }

    override fun evict(key: String) {
        try {
            redis.delete(key)
        } catch (ex: Exception) {
            log.warn("Redis EVICT failed for key '{}': {}", key, ex.message)
        }
    }
}
