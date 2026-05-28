package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.topic.AdminTaskStatsRepository
import com.ureka.play4change.domain.topic.TaskQuestionStats
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class JdbcAdminTaskStatsRepository(private val jdbc: JdbcTemplate) : AdminTaskStatsRepository {

    override fun getStatsByTemplateIds(templateIds: List<String>): Map<String, TaskQuestionStats> {
        if (templateIds.isEmpty()) return emptyMap()
        val placeholders = templateIds.joinToString(",") { "?" }
        val sql = """
            SELECT
                ta.task_template_id,
                COUNT(*)                                                          AS total_attempts,
                COUNT(*) FILTER (WHERE ta.is_correct = true)                     AS success_count,
                COALESCE(
                    COUNT(*) FILTER (WHERE ta.is_correct = true)::numeric
                    / NULLIF(COUNT(*), 0), 0)                                     AS success_rate,
                COALESCE(AVG(ta.points_awarded) FILTER (WHERE ta.status != 'PENDING'), 0) AS avg_points
            FROM task_assignments ta
            WHERE ta.task_template_id IN ($placeholders)
              AND ta.status != 'PENDING'
            GROUP BY ta.task_template_id
        """.trimIndent()
        return jdbc.query(
            sql,
            templateIds.toTypedArray(),
            { rs, _ ->
                rs.getString("task_template_id") to TaskQuestionStats(
                    totalAttempts = rs.getInt("total_attempts"),
                    successCount = rs.getInt("success_count"),
                    successRate = rs.getDouble("success_rate"),
                    avgPointsAwarded = rs.getDouble("avg_points")
                )
            }
        ).toMap()
    }
}
