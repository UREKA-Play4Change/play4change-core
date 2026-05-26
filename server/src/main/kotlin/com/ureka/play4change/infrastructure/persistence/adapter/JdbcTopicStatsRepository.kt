package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.topic.TopicStats
import com.ureka.play4change.domain.topic.TopicStatsRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class JdbcTopicStatsRepository(private val jdbc: JdbcTemplate) : TopicStatsRepository {

    companion object {
        private val ZERO = TopicStats(
            enrolledUsers = 0,
            completionRate = 0.0,
            averageScore = 0.0,
            activeUsers = 0
        )

        // Single combined query — one DB round-trip per topic.
        // LEFT JOIN so topics with no task_assignments still return the enrollment counts.
        private val SINGLE_SQL = """
            SELECT
                COUNT(DISTINCT e.id)                                                                AS enrolled_users,
                COALESCE(
                    COUNT(DISTINCT CASE WHEN e.status = 'COMPLETED' THEN e.user_id END)::numeric
                    / NULLIF(COUNT(DISTINCT e.user_id), 0), 0)                                     AS completion_rate,
                COALESCE(AVG(ta.points_awarded) FILTER (WHERE ta.points_awarded > 0), 0)           AS average_score,
                COUNT(DISTINCT CASE WHEN e.status = 'ACTIVE'
                    AND e.last_activity_at > NOW() - INTERVAL '7 days'
                    THEN e.user_id END)                                                             AS active_users
            FROM enrollments e
            LEFT JOIN task_assignments ta ON ta.enrollment_id = e.id
            WHERE e.topic_id = ?
        """.trimIndent()

        // Batch variant — one DB round-trip for N topics.
        // Placeholders are filled in dynamically; topics with no enrollments are absent from results.
        private fun batchSql(n: Int): String {
            val placeholders = (1..n).joinToString(",") { "?" }
            return """
                SELECT
                    e.topic_id,
                    COUNT(DISTINCT e.id)                                                                AS enrolled_users,
                    COALESCE(
                        COUNT(DISTINCT CASE WHEN e.status = 'COMPLETED' THEN e.user_id END)::numeric
                        / NULLIF(COUNT(DISTINCT e.user_id), 0), 0)                                     AS completion_rate,
                    COALESCE(AVG(ta.points_awarded) FILTER (WHERE ta.points_awarded > 0), 0)           AS average_score,
                    COUNT(DISTINCT CASE WHEN e.status = 'ACTIVE'
                        AND e.last_activity_at > NOW() - INTERVAL '7 days'
                        THEN e.user_id END)                                                             AS active_users
                FROM enrollments e
                LEFT JOIN task_assignments ta ON ta.enrollment_id = e.id
                WHERE e.topic_id IN ($placeholders)
                GROUP BY e.topic_id
            """.trimIndent()
        }
    }

    override fun getForTopic(topicId: String): TopicStats =
        jdbc.query(SINGLE_SQL, { rs, _ ->
            TopicStats(
                enrolledUsers = rs.getInt("enrolled_users"),
                completionRate = rs.getDouble("completion_rate"),
                averageScore = rs.getDouble("average_score"),
                activeUsers = rs.getInt("active_users")
            )
        }, topicId).firstOrNull() ?: ZERO

    override fun getForTopics(topicIds: List<String>): Map<String, TopicStats> {
        if (topicIds.isEmpty()) return emptyMap()
        return jdbc.query(
            batchSql(topicIds.size),
            topicIds.toTypedArray(),
            { rs, _ ->
                rs.getString("topic_id") to TopicStats(
                    enrolledUsers = rs.getInt("enrolled_users"),
                    completionRate = rs.getDouble("completion_rate"),
                    averageScore = rs.getDouble("average_score"),
                    activeUsers = rs.getInt("active_users")
                )
            }
        ).toMap()
    }
}
