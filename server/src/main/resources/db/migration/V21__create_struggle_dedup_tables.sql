-- Struggle deduplication tables used by the ai-agent PgVectorDeduplicationService.
-- struggle_events stores the embedding of a struggle context for similarity search.
-- adaptive_branches records generated branches so they can be reused for similar future struggles.

CREATE TABLE struggle_events (
    id        VARCHAR(36)  PRIMARY KEY,
    embedding vector(1024)
);

CREATE INDEX idx_struggle_events_embedding
    ON struggle_events USING ivfflat (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

CREATE TABLE adaptive_branches (
    id                VARCHAR(36)  PRIMARY KEY,
    struggle_event_id VARCHAR(36)  NOT NULL REFERENCES struggle_events(id) ON DELETE CASCADE,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_adaptive_branches_event ON adaptive_branches(struggle_event_id);
