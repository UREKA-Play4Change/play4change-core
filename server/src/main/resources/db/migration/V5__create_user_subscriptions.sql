-- V5__create_user_subscriptions.sql
CREATE TABLE user_subscriptions (
    id          VARCHAR(36)  PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    course_id   VARCHAR(36)  NOT NULL REFERENCES courses(id),
    module_id   VARCHAR(36)  NOT NULL REFERENCES course_modules(id),
    enrolled_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
);

CREATE UNIQUE INDEX idx_user_subscription_unique
    ON user_subscriptions (user_id, module_id);
