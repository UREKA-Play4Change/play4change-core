package com.ureka.play4change.infrastructure.scheduler

import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class StuckGenerationWatchdogJob(
    private val topicRepository: TopicRepository,
    @Value("\${scheduler.watchdog.stuck-threshold-minutes:5}") private val thresholdMinutes: Long
) {
    private val logger = LoggerFactory.getLogger(StuckGenerationWatchdogJob::class.java)

    @Scheduled(fixedRateString = "\${scheduler.watchdog.rate-ms:120000}")
    fun resetStuckGenerations() {
        val cutoff = OffsetDateTime.now().minusMinutes(thresholdMinutes)
        val stuck = topicRepository.findStuckGenerating(cutoff)
        if (stuck.isEmpty()) return
        logger.warn("Resetting ${stuck.size} stuck GENERATING topic(s) to FAILED")
        stuck.forEach { topicRepository.updateStatus(it.id, TopicStatus.FAILED) }
    }
}
