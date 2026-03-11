package com.ureka.play4change.model

data class AdaptiveBranch(
    val branchId: String,
    val subtasks: List<GeneratedTask>,
    val reuseStrategy: ReuseStrategy,
    val reusedFromBranchId: String?  // null if fully generated fresh
)

enum class ReuseStrategy {
    FULL_REUSE,      // similarity > 0.90
    PARTIAL_REUSE,   // similarity > 0.65
    FRESH_GENERATION // similarity < 0.65 or no match found
}