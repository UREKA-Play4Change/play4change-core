-- Add language column to task_templates.
-- Existing rows default to 'en' (the original source language).
ALTER TABLE task_templates
    ADD COLUMN language VARCHAR(10) NOT NULL DEFAULT 'en';

-- The original unique index enforced one current template per (module, day, pool).
-- With multi-language support, each language gets its own current slot.
DROP INDEX idx_task_pool_current;

CREATE UNIQUE INDEX idx_task_pool_current
    ON task_templates (module_id, day_index, pool_index, language)
    WHERE is_current = TRUE;

CREATE INDEX idx_task_templates_language ON task_templates(language);
