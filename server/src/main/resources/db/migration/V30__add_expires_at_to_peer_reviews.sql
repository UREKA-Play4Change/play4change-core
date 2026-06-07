ALTER TABLE peer_reviews ADD COLUMN expires_at TIMESTAMPTZ;

UPDATE peer_reviews
    SET expires_at = assigned_at + INTERVAL '48 hours'
    WHERE expires_at IS NULL;

ALTER TABLE peer_reviews
    ALTER COLUMN expires_at SET NOT NULL,
    ALTER COLUMN expires_at SET DEFAULT (now() + INTERVAL '48 hours');

CREATE INDEX idx_peer_review_expired
    ON peer_reviews(expires_at)
    WHERE verdict IS NULL;
