-- infra/postgres/init.sql
-- Runs ONCE on first container start, before Flyway migrations.
-- ONLY enable extensions here. Tables are created by Flyway.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";