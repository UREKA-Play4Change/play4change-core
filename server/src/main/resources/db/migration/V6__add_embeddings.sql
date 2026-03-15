-- V6__add_embeddings.sql
CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE task_templates
    ADD COLUMN IF NOT EXISTS embedding vector(1024);

CREATE INDEX IF NOT EXISTS idx_task_embedding
    ON task_templates USING ivfflat (embedding vector_cosine_ops);
