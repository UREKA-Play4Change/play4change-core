ALTER TABLE adaptive_tasks
    ADD COLUMN IF NOT EXISTS options         JSONB,
    ADD COLUMN IF NOT EXISTS correct_answer  INT,
    ADD COLUMN IF NOT EXISTS selected_option INT,
    ADD COLUMN IF NOT EXISTS option_order    JSONB;
