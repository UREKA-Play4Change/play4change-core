package com.ureka.play4change.web.admin

import com.ureka.play4change.application.port.TopicUseCase
import jakarta.validation.Valid
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.admin.dto.LearningGraphEdgeResponse
import com.ureka.play4change.web.admin.dto.LearningGraphResponse
import com.ureka.play4change.web.admin.dto.PrerequisiteTopicResponse
import com.ureka.play4change.web.admin.dto.SetPrerequisitesRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin")
class AdminTopicPrerequisiteController(
    private val topicUseCase: TopicUseCase
) {

    @GetMapping("/topics/{id}/prerequisites")
    fun getPrerequisites(@PathVariable id: String): ResponseEntity<List<PrerequisiteTopicResponse>> =
        topicUseCase.getPrerequisites(id).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { topics -> ResponseEntity.ok(topics.map { PrerequisiteTopicResponse.from(it) }) }
        )

    @PostMapping("/topics/{id}/prerequisites")
    fun setPrerequisites(
        @PathVariable id: String,
        @Valid @RequestBody request: SetPrerequisitesRequest
    ): ResponseEntity<List<PrerequisiteTopicResponse>> =
        topicUseCase.setPrerequisites(id, request.prerequisiteIds).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { topics -> ResponseEntity.ok(topics.map { PrerequisiteTopicResponse.from(it) }) }
        )

    @GetMapping("/learning-graph")
    fun getLearningGraph(): ResponseEntity<LearningGraphResponse> {
        val graph = topicUseCase.getLearningGraph()
        return ResponseEntity.ok(
            LearningGraphResponse(
                edges = graph.edges.map { LearningGraphEdgeResponse(it.topicId, it.prerequisiteTopicId) }
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
