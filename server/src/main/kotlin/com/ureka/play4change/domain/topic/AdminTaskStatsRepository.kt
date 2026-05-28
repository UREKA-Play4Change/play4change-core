package com.ureka.play4change.domain.topic

interface AdminTaskStatsRepository {
    fun getStatsByTemplateIds(templateIds: List<String>): Map<String, TaskQuestionStats>
}
