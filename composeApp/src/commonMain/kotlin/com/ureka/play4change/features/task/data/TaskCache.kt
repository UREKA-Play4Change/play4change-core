package com.ureka.play4change.features.task.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours

/**
 * In-memory TTL cache for API responses, keyed by endpoint URL.
 *
 * - Thread-safe via [ConcurrentHashMap]-equivalent (HashMap + [Mutex] per key).
 * - Default TTL: 6 hours.
 * - Lost on app kill — acceptable because [BackgroundFetchWorker] re-warms it.
 * - Concurrent reads for the same key are deduplicated: only one [fetch] is in-flight at a time.
 */
class TaskCache(
    private val defaultTtlHours: Long = DEFAULT_TTL_HOURS,
    private val nowProvider: () -> Instant = { Clock.System.now() }
) {

    private data class Entry(val data: String, val expiresAt: Instant)

    private val cache = HashMap<String, Entry>()
    private val keyMutexes = HashMap<String, Mutex>()
    private val globalMutex = Mutex()

    /**
     * Returns the cached value for [key] if it is still fresh, or calls [fetch],
     * stores the result with a [defaultTtlHours]-hour TTL, and returns it.
     * Concurrent calls for the same [key] are serialised so [fetch] runs at most once.
     */
    suspend fun getOrFetch(key: String, fetch: suspend () -> String): String {
        // Fast path: cache hit outside the lock
        cache[key]?.takeIf { it.expiresAt > nowProvider() }?.let { return it.data }

        val keyMutex = globalMutex.withLock {
            keyMutexes.getOrPut(key) { Mutex() }
        }
        return keyMutex.withLock {
            // Double-check after acquiring per-key lock
            cache[key]?.takeIf { it.expiresAt > nowProvider() }?.let { return@withLock it.data }
            val result = fetch()
            cache[key] = Entry(result, nowProvider().plus(defaultTtlHours.hours))
            result
        }
    }

    /** Returns the cached value for [key] without triggering a fetch; null if absent or expired. */
    fun get(key: String): String? =
        cache[key]?.takeIf { it.expiresAt > nowProvider() }?.data

    /** Removes the cached entry for [key]. */
    fun invalidate(key: String) {
        cache.remove(key)
    }

    private companion object {
        const val DEFAULT_TTL_HOURS = 6L
    }
}
