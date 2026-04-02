package com.ureka.play4change.web.user.dto

import com.ureka.play4change.application.port.RoadmapNode

data class RoadmapNodeResponse(
    val dayIndex: Int,
    val title: String,
    val status: String,
    val isAdaptive: Boolean,
    val assignmentId: String?,
    val pointsAwarded: Int?
) {
    companion object {
        fun from(node: RoadmapNode) = RoadmapNodeResponse(
            dayIndex = node.dayIndex,
            title = node.title,
            status = node.status.name,
            isAdaptive = node.isAdaptive,
            assignmentId = node.assignmentId,
            pointsAwarded = node.pointsAwarded
        )
    }
}
