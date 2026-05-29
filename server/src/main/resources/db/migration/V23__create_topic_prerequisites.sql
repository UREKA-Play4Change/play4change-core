-- Phase 09: Learning Path DAG
-- Join table storing directed prerequisite edges: topic_id requires prerequisite_topic_id

CREATE TABLE topic_prerequisites (
    id                   VARCHAR(36)  NOT NULL,
    topic_id             VARCHAR(36)  NOT NULL,
    prerequisite_topic_id VARCHAR(36) NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_topic_prerequisites PRIMARY KEY (id),
    CONSTRAINT uq_topic_prerequisite UNIQUE (topic_id, prerequisite_topic_id),
    CONSTRAINT fk_tp_topic      FOREIGN KEY (topic_id)              REFERENCES topics(id) ON DELETE CASCADE,
    CONSTRAINT fk_tp_prereq     FOREIGN KEY (prerequisite_topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

CREATE INDEX idx_tp_topic_id  ON topic_prerequisites (topic_id);
CREATE INDEX idx_tp_prereq_id ON topic_prerequisites (prerequisite_topic_id);
