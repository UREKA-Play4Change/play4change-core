-- V2__create_task_templates.sql
CREATE TABLE course_modules (
                                id          VARCHAR(36)  PRIMARY KEY,
                                course_id   VARCHAR(36)  NOT NULL REFERENCES courses(id),
                                order_index INT          NOT NULL DEFAULT 0,
                                title       VARCHAR(255) NOT NULL,
                                topic       VARCHAR(255) NOT NULL,
                                objective   TEXT         NOT NULL
);

CREATE TABLE task_templates (
                                id                      VARCHAR(36)  PRIMARY KEY,
                                module_id               VARCHAR(36)  NOT NULL REFERENCES course_modules(id),
                                day_index               INT          NOT NULL DEFAULT 0,
                                pool_index              INT          NOT NULL DEFAULT 0,
                                title                   VARCHAR(255) NOT NULL,
                                description             TEXT         NOT NULL,
                                hint                    TEXT,
                                task_type               VARCHAR(30)  NOT NULL DEFAULT 'MULTIPLE_CHOICE',
                                points_reward           INT          NOT NULL DEFAULT 20,
                                options                 JSONB,
                                correct_answer          INT,
                                requires_ai_validation  BOOLEAN      NOT NULL DEFAULT FALSE,
                                created_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_task_pool
    ON task_templates (module_id, day_index, pool_index);