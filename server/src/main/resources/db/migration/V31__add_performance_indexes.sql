-- Partial index for PENDING_REVIEW assignments: speeds up findByTopicAndTypeAndStatusExcludingUser
-- which runs on every peer review assignment request.
CREATE INDEX idx_assignments_pending_review
    ON task_assignments(enrollment_id, task_type, status)
    WHERE status = 'PENDING_REVIEW';

-- Partial index for submitted assignments: speeds up COUNT-based badge eligibility checks
-- and enrollment completion checks (submitted_at IS NOT NULL filter on enrollment_id).
CREATE INDEX idx_assignments_enrollment_submitted
    ON task_assignments(enrollment_id)
    WHERE submitted_at IS NOT NULL;

-- Partial index for COMPLETED adaptive branches: speeds up the vector similarity search
-- in PgVectorDeduplicationService which filters on status = 'COMPLETED' before ranking.
CREATE INDEX idx_adaptive_branches_completed
    ON adaptive_branches(status)
    WHERE status = 'COMPLETED';
