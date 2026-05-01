-- Task 2.6: MicroCompetence domain model and Badge issuance
-- micro_competences: topic-scoped value object, one per topic
-- user_badges: earned badge records, unique per (user, competence)

CREATE TABLE micro_competences (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    topic_id    VARCHAR(36)  NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    icon_url    TEXT,
    UNIQUE (topic_id)
);

CREATE INDEX idx_micro_competences_topic ON micro_competences(topic_id);

CREATE TABLE user_badges (
    id                  VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id             VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    micro_competence_id VARCHAR(36)  NOT NULL REFERENCES micro_competences(id) ON DELETE CASCADE,
    earned_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, micro_competence_id)
);

CREATE INDEX idx_user_badges_user ON user_badges(user_id);
