package com.ureka.play4change.domain.struggle

interface StruggleRepository {
    fun findById(id: String): StruggleSession?
    fun findOpenByEnrollmentId(enrollmentId: String): StruggleSession?
    fun findAllByEnrollmentId(enrollmentId: String): List<StruggleSession>
    fun save(session: StruggleSession): StruggleSession
    fun findAdaptiveTasksByTopicId(topicId: String): List<AdaptiveTaskAdminView>
    fun findPathStatsByTopicId(topicId: String): List<StrugglePathStats>
    fun findAdaptiveTaskById(taskId: String): AdaptiveTask?
    fun findAdaptiveTaskViewById(taskId: String): AdaptiveTaskAdminView?
    fun saveAdaptiveTask(task: AdaptiveTask): AdaptiveTask
}
