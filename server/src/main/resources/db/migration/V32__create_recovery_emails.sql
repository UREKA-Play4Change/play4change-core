-- Recovery emails: users can register backup email addresses.
-- When a magic link is requested for a verified recovery email, the system authenticates
-- the request as the linked account instead of creating a new one.
-- Tokens expire in 24 hours; the UNIQUE constraint prevents the same email from being
-- a recovery address for more than one account.

CREATE TABLE recovery_emails (
    id               VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id          VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email            VARCHAR(255) NOT NULL,
    verified         BOOLEAN      NOT NULL DEFAULT FALSE,
    token_hash       VARCHAR(255),       -- SHA-256 of verification token; NULL after verified
    token_expires_at TIMESTAMPTZ,        -- NULL after verified
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (email)
);

CREATE INDEX idx_recovery_emails_user         ON recovery_emails(user_id);
CREATE INDEX idx_recovery_emails_verified     ON recovery_emails(email) WHERE verified = TRUE;