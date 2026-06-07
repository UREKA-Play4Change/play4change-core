ALTER TABLE task_assignments
    ADD COLUMN correct_answer_index INT;
-- Nullable intentionally: existing rows fall back to template lookup at submission time.
