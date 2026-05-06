-- Task 3.4: User preferences — timezone
-- preferred_language already exists; only timezone is new.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(60) DEFAULT NULL;
