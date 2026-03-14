-- infra/postgres/init.sql
-- Runs once on first container start.
-- Enables pgvector extension so your embedding columns work.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- gen_random_uuid() support