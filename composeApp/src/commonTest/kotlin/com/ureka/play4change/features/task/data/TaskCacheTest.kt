package com.ureka.play4change.features.task.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskCacheTest {

    /** Controllable time source for TTL assertions. */
    private class TestClock(var nowMs: Long) {
        fun advanceBy(seconds: Long) { nowMs += seconds * 1000 }
        val provider: () -> Long = { nowMs }
    }

    @Test
    fun `fresh cache entry is returned without a network call`() = runTest {
        val clock = TestClock(0L)
        val cache = TaskCache(nowMs = clock.provider)
        var fetchCount = 0

        // First call — populates the cache
        cache.getOrFetch("key") { fetchCount++; "data" }
        // Second call — should hit the cache
        val result = cache.getOrFetch("key") { fetchCount++; "data-new" }

        assertEquals("data", result)
        assertEquals(1, fetchCount, "fetch should have been called exactly once")
    }

    @Test
    fun `expired cache entry triggers a network fetch`() = runTest {
        val clock = TestClock(0L)
        val cache = TaskCache(defaultTtlHours = 6, nowMs = clock.provider)
        var fetchCount = 0

        cache.getOrFetch("key") { fetchCount++; "data-v1" }

        // Advance past the 6-hour TTL
        clock.advanceBy(6 * 3600 + 1)

        val result = cache.getOrFetch("key") { fetchCount++; "data-v2" }

        assertEquals("data-v2", result)
        assertEquals(2, fetchCount, "fetch should have been called again after TTL expiry")
    }

    @Test
    fun `cache returns null for a key that was never fetched`() {
        val cache = TaskCache()
        assertNull(cache.get("unknown-key"))
    }

    @Test
    fun `two concurrent reads for the same key trigger only one fetch`() = runTest {
        val cache = TaskCache()
        var fetchCount = 0

        // Launch 10 coroutines that all request the same key simultaneously
        val results = (1..10).map {
            async {
                cache.getOrFetch("shared-key") {
                    fetchCount++
                    "value"
                }
            }
        }.awaitAll()

        assertTrue(results.all { it == "value" }, "all coroutines should see the same result")
        assertEquals(1, fetchCount, "fetch should have been called exactly once despite concurrent access")
    }
}
