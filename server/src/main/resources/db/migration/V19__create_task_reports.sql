-- Task 3.3: Bad question reporting
-- Learners flag incorrect questions; admins review, correct, or dismiss.

CREATE TABLE task_reports (
    id              VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    task_template_id VARCHAR(36) NOT NULL REFERENCES task_templates(id) ON DELETE CASCADE,
    user_id         VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason          TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reported_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ,
    CONSTRAINT uq_task_report_user_task UNIQUE (user_id, task_template_id)
);

CREATE INDEX idx_task_reports_status ON task_reports(status);
CREATE INDEX idx_task_reports_task   ON task_reports(task_template_id);
