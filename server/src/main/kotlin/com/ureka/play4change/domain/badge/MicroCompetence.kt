package com.ureka.play4change.domain.badge

data class MicroCompetence(
    val id: String,
    val name: String,
    val description: String,
    val topicId: String,
    val iconUrl: String? = null
) {
    init {
        require(name.isNotBlank()) { "MicroCompetence name must not be blank" }
        require(description.isNotBlank()) { "MicroCompetence description must not be blank" }
    }
}
