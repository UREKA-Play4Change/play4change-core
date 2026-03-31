CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id                 VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    email              VARCHAR(255) NOT NULL UNIQUE,
    name               VARCHAR(255),
    avatar_url         TEXT,
    role               VARCHAR(10)  NOT NULL DEFAULT 'USER',         -- USER | ADMIN
    provider           VARCHAR(20)  NOT NULL,                        -- MAGIC_LINK | GOOGLE | FACEBOOK
    provider_id        VARCHAR(255),
    preferred_language VARCHAR(10)  NOT NULL DEFAULT 'en',
    audience_level     VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',     -- BEGINNER | INTERMEDIATE | ADVANCED
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_id)
);

CREATE TABLE magic_link_tokens (
    id         VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    token      VARCHAR(255) NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    user_id     VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id   VARCHAR(36)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_magic_link_token   ON magic_link_tokens(token);
CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_family     ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_user       ON refresh_tokens(user_id);
