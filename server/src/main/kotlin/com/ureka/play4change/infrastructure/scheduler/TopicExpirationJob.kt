package com.ureka.play4change.infrastructure.scheduler

import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TopicExpirationJob(
    private val topicRepository: TopicRepository
) {
    private val logger = LoggerFactory.getLogger(TopicExpirationJob::class.java)

    @Scheduled(fixedRateString = "\${scheduler.expiration.rate-ms:60000}")
    fun expireTopics() {
        val expired = topicRepository.findActiveExpired()
        if (expired.isEmpty()) return
        logger.info("Expiring ${expired.size} topic(s)")
        expired.forEach { topicRepository.updateStatus(it.id, TopicStatus.EXPIRED) }
    }
}
