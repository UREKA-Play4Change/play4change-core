    package com.ureka.play4change.dedup

import com.ureka.play4change.model.ReuseStrategy
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class PgVectorDeduplicationService(
    private val jdbc: JdbcTemplate,
    private val meterRegistry: MeterRegistry,
    @Value("\${ai.dedup.full-reuse-threshold:0.90}") private val fullReuseThreshold: Double,
    @Value("\${ai.dedup.partial-reuse-threshold:0.65}") private val partialReuseThreshold: Double,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isDuplicate(embedding: FloatArray, moduleId: String, threshold: Double = fullReuseThreshold): Boolean {
        val result = jdbc.queryForList(
            """
            SELECT 1 FROM task_templates
            WHERE module_id = ?
            AND 1 - (embedding <=> ?::vector) > ?
            LIMIT 1
            """.trimIndent(),
            moduleId, embedding.toVectorString(), threshold
        )
        val isDuplicate = result.isNotEmpty()
        if (isDuplicate) {
            meterRegistry.counter("ai.dedup.duplicates_skipped", "module_id", moduleId).increment()
            log.debug("Duplicate detected for module=$moduleId")
        }
        return isDuplicate
    }

    fun findSimilarStruggle(struggleEmbedding: FloatArray): SimilarityMatch {
        val vectorStr = struggleEmbedding.toVectorString()
        val results = jdbc.queryForList(
            """
            SELECT ab.id as branch_id, 1 - (se.embedding <=> ?::vector) as similarity
            FROM struggle_events se
            JOIN adaptive_branches ab ON ab.struggle_event_id = se.id
            WHERE ab.status = 'COMPLETED'
            ORDER BY se.embedding <=> ?::vector
            LIMIT 1
            """.trimIndent(),
            vectorStr, vectorStr
        )

        if (results.isEmpty()) {
            log.debug("No similar struggle found — generating fresh")
            return SimilarityMatch(branchId = null, similarity = 0.0, strategy = ReuseStrategy.FRESH_GENERATION)
        }

        val similarity = results[0]["similarity"] as Double
        val branchId = results[0]["branch_id"] as String
        val strategy = when {
            similarity > fullReuseThreshold -> ReuseStrategy.FULL_REUSE
            similarity > partialReuseThreshold -> ReuseStrategy.PARTIAL_REUSE
            else -> ReuseStrategy.FRESH_GENERATION
        }

        meterRegistry.counter("ai.struggle.reuse_strategy", "strategy", strategy.name).increment()
        log.info("Struggle similarity=$similarity strategy=$strategy branchId=$branchId")
        return SimilarityMatch(branchId = branchId, similarity = similarity, strategy = strategy)
    }

    fun storeStruggleEmbedding(struggleEventId: String, embedding: FloatArray) {
        jdbc.update(
            "UPDATE struggle_events SET embedding = ?::vector WHERE id = ?",
            embedding.toVectorString(), struggleEventId
        )
    }

    private fun FloatArray.toVectorString(): String =
        joinToString(prefix = "[", postfix = "]", separator = ",")
}

data class SimilarityMatch(
    val branchId: String?,
    val similarity: Double,
    val strategy: ReuseStrategy
)