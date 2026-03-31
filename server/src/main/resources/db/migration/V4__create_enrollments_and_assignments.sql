CREATE TABLE enrollments (
    id                  VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id             VARCHAR(36)  NOT NULL REFERENCES users(id),
    topic_id            VARCHAR(36)  NOT NULL REFERENCES topics(id),
    topic_module_id     VARCHAR(36)  NOT NULL REFERENCES topic_modules(id),
    enrolled_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status              VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | COMPLETED | EXPIRED | PAUSED
    current_day_index   INT          NOT NULL DEFAULT 0,
    total_points_earned INT          NOT NULL DEFAULT 0,
    streak_days         INT          NOT NULL DEFAULT 0,
    last_activity_at    TIMESTAMPTZ,
    UNIQUE (user_id, topic_module_id)
);

CREATE INDEX idx_enrollments_user   ON enrollments(user_id);
CREATE INDEX idx_enrollments_topic  ON enrollments(topic_id);
CREATE INDEX idx_enrollments_active ON enrollments(user_id, status);

CREATE TABLE task_assignments (
    id                    VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    enrollment_id         VARCHAR(36)  NOT NULL REFERENCES enrollments(id),
    user_id               VARCHAR(36)  NOT NULL REFERENCES users(id),   -- denormalized for query performance
    task_template_id      VARCHAR(36)  NOT NULL REFERENCES task_templates(id),
    task_template_version INT          NOT NULL DEFAULT 1,
    task_type             VARCHAR(20)  NOT NULL,
    assigned_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    due_at                TIMESTAMPTZ  NOT NULL,
    submitted_at          TIMESTAMPTZ,
    status                VARCHAR(10)  NOT NULL DEFAULT 'PENDING',   -- PENDING | SUBMITTED | LATE | SKIPPED
    selected_option       INT,
    is_correct            BOOLEAN,
    points_awarded        INT          NOT NULL DEFAULT 0,
    option_order          JSONB,
    wrong_attempt_count   INT          NOT NULL DEFAULT 0,
    photo_url             TEXT,
    UNIQUE (enrollment_id, task_template_id)
);

CREATE INDEX idx_assignments_enrollment ON task_assignments(enrollment_id);
CREATE INDEX idx_assignments_user       ON task_assignments(user_id);
CREATE INDEX idx_assignments_pending    ON task_assignments(enrollment_id, status) WHERE status = 'PENDING';
