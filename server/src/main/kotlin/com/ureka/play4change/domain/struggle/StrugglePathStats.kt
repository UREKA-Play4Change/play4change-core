package com.ureka.play4change.domain.struggle

data class StrugglePathStats(
    val originalTaskTemplateId: String,
    val errorPattern: ErrorPattern,
    val totalSessions: Int
)
