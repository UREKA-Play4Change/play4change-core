-- No schema changes. Placeholder for first-admin promotion instructions.
-- To promote a user to ADMIN, run the following against the live database
-- after they have signed in at least once:
--
-- UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';
--
-- Flyway requires at least one executable statement per migration file.
SELECT 1;
