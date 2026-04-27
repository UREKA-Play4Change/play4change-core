-- Add category column to topics.
-- Existing rows default to empty string (not null so the constraint is consistent).
ALTER TABLE topics ADD COLUMN category VARCHAR(100) NOT NULL DEFAULT '';
