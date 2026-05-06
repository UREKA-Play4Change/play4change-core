package com.ureka.play4change.web.admin.dto

data class CorrectTaskReportRequest(
    val correctedTitle: String,
    val correctedOptions: List<String>,
    val correctAnswerIndex: Int
)
