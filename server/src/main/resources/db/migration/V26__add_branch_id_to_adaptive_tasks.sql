-- Links each adaptive task instance back to the reusable adaptive_branches entry.
-- Nullable: tasks created before this migration and fresh-generated tasks without a stored branch keep NULL.
ALTER TABLE adaptive_tasks
    ADD COLUMN IF NOT EXISTS branch_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_adaptive_tasks_branch
    ON adaptive_tasks(branch_id)
    WHERE branch_id IS NOT NULL;
