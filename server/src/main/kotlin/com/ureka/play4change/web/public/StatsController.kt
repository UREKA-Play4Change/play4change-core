package com.ureka.play4change.web.public

import com.ureka.play4change.auth.adapter.outbound.persistence.spring.UserJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TopicJpaRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class PublicStatsResponse(
    val totalUsers: Long,
    val activeTopics: Long,
    val tasksCompleted: Long
)

@RestController
@RequestMapping("/api/stats")
class StatsController(
    private val userJpaRepository: UserJpaRepository,
    private val topicJpaRepository: TopicJpaRepository
) {
    @GetMapping("/public")
    fun publicStats(): ResponseEntity<PublicStatsResponse> =
        ResponseEntity.ok(
            PublicStatsResponse(
                totalUsers = userJpaRepository.count(),
                activeTopics = topicJpaRepository.countByStatus("ACTIVE"),
                tasksCompleted = 0L
            )
        )
}
