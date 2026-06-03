package com.ureka.play4change.domain.struggle

import java.time.OffsetDateTime

data class AdaptiveTaskAdminView(
    val task: AdaptiveTask,
    val sessionId: String,
    val sessionStatus: StruggleStatus,
    val errorPattern: ErrorPattern,
    val sessionDetectedAt: OffsetDateTime,
    val enrollmentId: String,
    val originalTaskTemplateId: String,
    val originalTaskTitle: String
)
