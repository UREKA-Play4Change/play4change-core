-- PENDING_REVIEW is 14 chars; original VARCHAR(10) is too narrow
ALTER TABLE task_assignments ALTER COLUMN status TYPE VARCHAR(20);
