package com.ureka.play4change.domain.enrollment

interface EnrollmentRepository {
    fun findById(id: String): Enrollment?
    fun findByUserIdAndTopicId(userId: String, topicId: String): Enrollment?
    fun findActiveByUserId(userId: String): List<Enrollment>
    fun findAssignmentById(id: String): TaskAssignment?
    fun findAssignmentByEnrollmentAndTemplate(enrollmentId: String, taskTemplateId: String): TaskAssignment?
    fun save(enrollment: Enrollment): Enrollment
    fun saveAssignment(assignment: TaskAssignment): TaskAssignment
}
