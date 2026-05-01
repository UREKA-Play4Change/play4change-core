package com.ureka.play4change.domain.enrollment

import java.security.MessageDigest
import java.util.Random

/**
 * Deterministic per-user option shuffle for anti-cheat.
 *
 * Seed = SHA-256(userId + taskId + enrollmentId), first 8 bytes interpreted as a big-endian Long.
 * The same three inputs always produce the same shuffle order, so a user always sees the same
 * option arrangement for a given task — but different users see different arrangements.
 */
object TaskShuffleSeed {

    private const val SEED_BYTES = 8
    private const val BITS_PER_BYTE = 8
    private const val BYTE_MASK = 0xFFL

    fun computeSeed(userId: String, taskId: String, enrollmentId: String): Long {
        val input = "$userId$taskId$enrollmentId".toByteArray(Charsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-256").digest(input)
        var seed = 0L
        for (i in 0 until SEED_BYTES) {
            seed = (seed shl BITS_PER_BYTE) or (hash[i].toLong() and BYTE_MASK)
        }
        return seed
    }

    fun shuffleOptions(optionCount: Int, userId: String, taskId: String, enrollmentId: String): List<Int> {
        if (optionCount == 0) return emptyList()
        val indices = (0 until optionCount).toMutableList()
        val seed = computeSeed(userId, taskId, enrollmentId)
        val random = Random(seed)
        for (i in indices.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = indices[i]
            indices[i] = indices[j]
            indices[j] = tmp
        }
        return indices
    }
}
