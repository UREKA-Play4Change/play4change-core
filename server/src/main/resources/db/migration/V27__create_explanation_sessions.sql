CREATE TABLE explanation_sessions (
    id                          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    enrollment_id               VARCHAR(36)  NOT NULL REFERENCES enrollments(id),
    original_task_assignment_id VARCHAR(36)  NOT NULL REFERENCES task_assignments(id),
    error_pattern               VARCHAR(30)  NOT NULL,
    explanation_text            TEXT,                                                          -- null while AI is still generating
    status                      VARCHAR(15)  NOT NULL DEFAULT 'GENERATING',                   -- GENERATING | ACTIVE | RESOLVED
    generated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at                 TIMESTAMPTZ
);

CREATE INDEX idx_explanation_enrollment_active
    ON explanation_sessions(enrollment_id)
    WHERE status IN ('GENERATING', 'ACTIVE');

CREATE TABLE explanation_messages (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    session_id  VARCHAR(36)  NOT NULL REFERENCES explanation_sessions(id) ON DELETE CASCADE,
    role        VARCHAR(10)  NOT NULL,                                                         -- USER | AI
    content     TEXT         NOT NULL,
    sent_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_explanation_messages_session ON explanation_messages(session_id);
