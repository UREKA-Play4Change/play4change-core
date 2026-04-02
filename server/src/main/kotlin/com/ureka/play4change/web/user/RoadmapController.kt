package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.RoadmapUseCase
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.user.dto.RoadmapNodeResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/topics")
class RoadmapController(private val roadmapUseCase: RoadmapUseCase) {

    @GetMapping("/{topicId}/roadmap")
    fun getRoadmap(
        @PathVariable topicId: String,
        @AuthenticationPrincipal userId: String,
        @RequestHeader(value = "X-Timezone", required = false) timezone: String?
    ): ResponseEntity<List<RoadmapNodeResponse>> =
        roadmapUseCase.getRoadmap(userId, topicId, timezone).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(it.map(RoadmapNodeResponse::from)) }
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
