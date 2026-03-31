CREATE TABLE topics (
    id                       VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title                    VARCHAR(255) NOT NULL,
    description              TEXT         NOT NULL,
    content_source_type      VARCHAR(10)  NOT NULL,      -- URL | PDF
    content_source_ref       TEXT         NOT NULL,      -- URL string or stored file reference
    raw_extracted_text       TEXT,                       -- stored for admin re-generation without re-upload
    task_count               INT          NOT NULL,
    subscription_window_days INT          NOT NULL DEFAULT 7,
    expires_at               TIMESTAMPTZ  NOT NULL,
    audience_level           VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    language                 VARCHAR(10)  NOT NULL DEFAULT 'en',
    status                   VARCHAR(15)  NOT NULL DEFAULT 'DRAFT',   -- DRAFT | GENERATING | ACTIVE | EXPIRED | FAILED
    created_by               VARCHAR(36)  NOT NULL REFERENCES users(id),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_topics_status     ON topics(status);
CREATE INDEX idx_topics_created_by ON topics(created_by);
CREATE INDEX idx_topics_expires_at ON topics(expires_at) WHERE status = 'ACTIVE';
