CREATE TABLE struggle_sessions (
    id                          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    enrollment_id               VARCHAR(36)  NOT NULL REFERENCES enrollments(id),
    original_task_assignment_id VARCHAR(36)  NOT NULL REFERENCES task_assignments(id),
    error_pattern               VARCHAR(30)  NOT NULL,   -- WRONG_CONCEPT | PARTIAL_UNDERSTANDING | READING_ERROR | TIME_PRESSURE
    attempt_count               INT          NOT NULL DEFAULT 2,
    detected_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at                 TIMESTAMPTZ,
    status                      VARCHAR(15)  NOT NULL DEFAULT 'OPEN'   -- OPEN | RESOLVED | ABANDONED
);

CREATE INDEX idx_struggle_enrollment ON struggle_sessions(enrollment_id);
CREATE INDEX idx_struggle_open       ON struggle_sessions(enrollment_id, status) WHERE status = 'OPEN';

CREATE TABLE adaptive_tasks (
    id                  VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    struggle_session_id VARCHAR(36)  NOT NULL REFERENCES struggle_sessions(id) ON DELETE CASCADE,
    title               VARCHAR(255) NOT NULL,
    description         TEXT         NOT NULL,
    hint                TEXT,
    points_reward       INT          NOT NULL DEFAULT 10,
    order_index         INT          NOT NULL,
    completed_at        TIMESTAMPTZ,
    is_correct          BOOLEAN,
    UNIQUE (struggle_session_id, order_index)
);

CREATE INDEX idx_adaptive_session ON adaptive_tasks(struggle_session_id);
