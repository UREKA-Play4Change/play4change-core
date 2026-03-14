package com.ureka.play4change.model

import com.ureka.play4change.domain.AudienceLevel

data class GenerationRequest(
    val courseId: String,
    val moduleId: String,
    val subjectDomain: String,       // e.g. "sustainability", "digital_literacy"
    val audienceLevel: AudienceLevel,
    val language: String,            // ISO 639-1: "en", "pt", "fr"
    val taskCount: Int,
    val moduleObjective: String,     // what the module aims to teach
    val existingEmbeddings: List<FloatArray> = emptyList() // for deduplication
)
