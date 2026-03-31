CREATE TABLE peer_reviews (
    id                       VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    submission_assignment_id VARCHAR(36)  NOT NULL REFERENCES task_assignments(id),
    reviewer_user_id         VARCHAR(36)  NOT NULL REFERENCES users(id),
    verdict                  VARCHAR(10),                         -- CORRECT | INCORRECT — NULL until reviewed
    comment                  TEXT,
    assigned_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_at              TIMESTAMPTZ,
    UNIQUE (submission_assignment_id, reviewer_user_id)
);

CREATE INDEX idx_peer_review_submission ON peer_reviews(submission_assignment_id);
CREATE INDEX idx_peer_review_reviewer   ON peer_reviews(reviewer_user_id);
-- Partial index for fast lookup of pending reviews per reviewer
CREATE INDEX idx_peer_review_pending    ON peer_reviews(reviewer_user_id) WHERE reviewed_at IS NULL;
