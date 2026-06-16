-- ============================================================
-- FixItNow -- Optional Seed Data (MySQL 8)
-- ============================================================
-- Inserts a handful of service categories and services so the "browse" pages
-- have content immediately. Run AFTER the schema exists, ONCE, on a fresh DB.
--
-- Users are intentionally NOT seeded -- register them through the app (sign-up),
-- which hashes passwords with BCrypt. To create an ADMIN for local testing,
-- register normally then promote the row:
--   UPDATE users SET user_type = 'ADMIN', is_verified = 1 WHERE email = 'you@example.com';
--
-- Usage (from this folder):
--   mysql -u root -p fixitnow_db < seed.sql
-- ============================================================

USE fixitnow_db;

INSERT INTO categories (category_type, description, is_active) VALUES
  ('Plumbing',   'Pipes, leaks, fittings, drainage',   1),
  ('Electrical', 'Wiring, fixtures, panels',           1),
  ('Carpentry',  'Furniture, doors, woodwork',         1),
  ('Painting',   'Interior and exterior painting',     1),
  ('Cleaning',   'Home and deep cleaning',             1);

-- category_id values below assume a fresh DB where the inserts above produced
-- ids 1..5. Adjust if your categories table already had rows.
INSERT INTO services (name, category_id, day_payment, description, is_active) VALUES
  ('Pipe Leak Repair',          1, 2500.00, 'Fix leaking pipes and joints',  1),
  ('Tap & Faucet Installation', 1, 1800.00, 'Install or replace taps',       1),
  ('Wiring & Rewiring',         2, 3000.00, 'Electrical wiring work',        1),
  ('Fan & Light Installation',  2, 1500.00, 'Install fans and light fixtures', 1),
  ('Furniture Assembly',        3, 2000.00, 'Assemble flat-pack furniture',  1),
  ('Interior Painting',         4, 3500.00, 'Paint interior walls',          1),
  ('Deep House Cleaning',       5, 4000.00, 'Full home deep clean',          1);
