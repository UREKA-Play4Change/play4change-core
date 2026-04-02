package com.ureka.play4change.application.port

interface CachePort {
    fun get(key: String): String?
    fun put(key: String, value: String, ttlSeconds: Long)
    fun evict(key: String)
}
