-- V3__create_user_tasks.sql
CREATE TABLE user_tasks (
                            id                  VARCHAR(36)  PRIMARY KEY,
                            user_id             VARCHAR(255) NOT NULL,
                            task_template_id    VARCHAR(36)  NOT NULL REFERENCES task_templates(id),
                            assigned_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
                            submitted_at        TIMESTAMPTZ,
                            status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                            selected_option     INT,
                            text_answer         TEXT,
                            is_correct          BOOLEAN,
                            points_awarded      INT          NOT NULL DEFAULT 0,
                            option_order        JSONB
);

CREATE UNIQUE INDEX idx_user_task_unique
    ON user_tasks (user_id, task_template_id);