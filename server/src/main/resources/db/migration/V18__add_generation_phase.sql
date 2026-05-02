-- Task 2.8: 4-phase generation pipeline
-- Adds currentPhase + phaseUpdatedAt to topics and creates the phase transition log table.

ALTER TABLE topics
    ADD COLUMN current_phase    VARCHAR(20) NOT NULL DEFAULT 'INGESTION',
    ADD COLUMN phase_updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Backfill current_phase for existing rows based on their status.
UPDATE topics SET current_phase =
    CASE status
        WHEN 'ACTIVE'     THEN 'ACTIVE'
        WHEN 'EXPIRED'    THEN 'ACTIVE'
        WHEN 'FAILED'     THEN 'FAILED'
        ELSE 'INGESTION'
    END;

CREATE TABLE topic_phase_log (
    id              VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    topic_id        VARCHAR(36)  NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    from_phase      VARCHAR(20)  NOT NULL,
    to_phase        VARCHAR(20)  NOT NULL,
    transitioned_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    duration_ms     BIGINT       NOT NULL
);

CREATE INDEX idx_topic_phase_log_topic ON topic_phase_log(topic_id);
