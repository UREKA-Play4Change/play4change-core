-- Task 2.5: batch N-instance generation per task
-- task_instances stores variant distractor sets for each task_template.
-- One template → N instances, selected per user via abs(seed) % N.

CREATE TABLE task_instances (
    id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    task_template_id VARCHAR(36)  NOT NULL REFERENCES task_templates(id) ON DELETE CASCADE,
    instance_index   INT          NOT NULL,
    options          JSONB        NOT NULL,
    correct_answer   INT          NOT NULL,
    UNIQUE (task_template_id, instance_index)
);

CREATE INDEX idx_task_instances_template ON task_instances (task_template_id);

-- Add the selected instance reference to task_assignments (nullable for backward compatibility).
ALTER TABLE task_assignments
    ADD COLUMN task_instance_id VARCHAR(36) REFERENCES task_instances(id);
