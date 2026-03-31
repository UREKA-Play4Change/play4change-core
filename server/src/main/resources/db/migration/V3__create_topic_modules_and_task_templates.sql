CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE topic_modules (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    topic_id    VARCHAR(36)  NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    order_index INT          NOT NULL DEFAULT 0,
    objective   TEXT         NOT NULL,
    UNIQUE (topic_id, order_index)
);

CREATE INDEX idx_topic_modules_topic ON topic_modules(topic_id);

CREATE TABLE task_templates (
    id             VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    module_id      VARCHAR(36)  NOT NULL REFERENCES topic_modules(id) ON DELETE CASCADE,
    day_index      INT          NOT NULL,
    pool_index     INT          NOT NULL DEFAULT 0,
    title          VARCHAR(255) NOT NULL,
    description    TEXT         NOT NULL,
    hint           TEXT,
    task_type      VARCHAR(20)  NOT NULL DEFAULT 'MULTIPLE_CHOICE',  -- MULTIPLE_CHOICE | TODO_ACTION
    points_reward  INT          NOT NULL DEFAULT 20,
    options        JSONB,
    correct_answer INT,
    version        INT          NOT NULL DEFAULT 1,
    is_current     BOOLEAN      NOT NULL DEFAULT TRUE,
    superseded_by  VARCHAR(36)  REFERENCES task_templates(id),
    embedding      vector(1024),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Only one current version per slot
CREATE UNIQUE INDEX idx_task_pool_current
    ON task_templates (module_id, day_index, pool_index)
    WHERE is_current = TRUE;

CREATE INDEX idx_task_templates_module  ON task_templates(module_id);
CREATE INDEX idx_task_templates_current ON task_templates(module_id, is_current);

-- ivfflat index for cosine similarity search — only index rows that have embeddings
CREATE INDEX idx_task_embedding
    ON task_templates USING ivfflat (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;
