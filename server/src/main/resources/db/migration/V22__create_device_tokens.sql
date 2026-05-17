CREATE TABLE device_tokens (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          VARCHAR(36) NOT NULL,
    token            TEXT        NOT NULL,
    platform         VARCHAR(10) NOT NULL,
    last_notified_at TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_device_tokens_platform CHECK (platform IN ('ANDROID', 'IOS')),
    CONSTRAINT uq_device_tokens_user_platform UNIQUE (user_id, platform)
);
