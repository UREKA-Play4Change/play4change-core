package com.ureka.play4change.web.admin

import com.ureka.play4change.application.port.BadgeQueryUseCase
import com.ureka.play4change.web.admin.dto.TopicBadgeStatsResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/topics")
class AdminBadgeController(private val badgeQueryUseCase: BadgeQueryUseCase) {

    @GetMapping("/{topicId}/badges")
    fun getTopicBadgeStats(@PathVariable topicId: String): ResponseEntity<TopicBadgeStatsResponse> =
        ResponseEntity.ok(TopicBadgeStatsResponse.from(badgeQueryUseCase.getTopicBadgeStats(topicId)))
}
