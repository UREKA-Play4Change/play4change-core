package com.ureka.play4change.domain.struggle

interface StruggleRepository {
    fun findById(id: String): StruggleSession?
    fun findOpenByEnrollmentId(enrollmentId: String): StruggleSession?
    fun findAllByEnrollmentId(enrollmentId: String): List<StruggleSession>
    fun countByEnrollmentIdAndOriginalAssignmentId(enrollmentId: String, originalAssignmentId: String): Int
    fun save(session: StruggleSession): StruggleSession
    fun findAdaptiveTasksByTopicId(topicId: String): List<AdaptiveTaskAdminView>
    fun findPathStatsByTopicId(topicId: String): List<StrugglePathStats>
    fun findAdaptiveTaskById(taskId: String): AdaptiveTask?
    fun findAdaptiveTaskViewById(taskId: String): AdaptiveTaskAdminView?
    fun saveAdaptiveTask(task: AdaptiveTask): AdaptiveTask
    /**
     * Returns all distinct branchIds that this enrollment has already used for the given
     * original task assignment. Used to exclude already-seen branches from similarity reuse
     * when spawning follow-up struggle sessions so the learner is never asked the same
     * adaptive questions twice.
     */
    fun findUsedBranchIdsByAssignment(enrollmentId: String, assignmentId: String): List<String>
}
