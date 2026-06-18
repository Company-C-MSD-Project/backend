-- ============================================================
-- FixItNow -- Catalog + Provider Seed Data (MySQL 8 / MariaDB)
-- ============================================================
-- Populates: service categories, sub-services (with LKR pricing), verified
-- service-provider accounts (one lead + extras per category), their map
-- locations, and their "My Service Cards" listings -- so the live browse /
-- provider pages have real, database-backed content (no hardcoded data).
--
-- Provider login password (all seeded accounts): Provider@123
--   stored as a BCrypt hash below; change per-account after seeding.
--
-- Safe to run once on a fresh DB. Re-running skips duplicate categories/users
-- (unique keys) but will duplicate sub-services / cards, so run ONCE.
--
-- Usage:  mysql -u root -p fixitnow_db < seed.sql       (or: sudo mysql fixitnow_db < seed.sql)
-- ============================================================

-- BCrypt('Provider@123'), strength 10 -- verified against Spring BCryptPasswordEncoder
SET @ph := '$2a$10$uVVZXtTc4n36rAbmQjHEhuWSBgi6IYRYj9Ifs1UNRH9Q4fqacwfH6';

-- ---------- 1) Categories ----------
INSERT INTO categories (category_type, description, is_active) VALUES
  ('Plumbing',   'Pipes, leaks, fittings, drainage and water systems', 1),
  ('Electrical', 'Wiring, fixtures, switchboards and power',            1),
  ('Cleaning',   'Home, deep and post-construction cleaning',          1),
  ('Masonry',    'Plastering, tiling, brickwork and concrete',         1),
  ('Carpentry',  'Furniture, doors, cabinets and woodwork',            1)
ON DUPLICATE KEY UPDATE description = VALUES(description), is_active = 1;

-- ---------- 2) Sub-services (services) ----------
INSERT INTO services (name, category_id, day_payment, description, is_active) VALUES
  ('Pipe Leak Repair',           (SELECT id FROM categories WHERE category_type='Plumbing'),   2500.00, 'Fix leaking pipes, joints and fittings',          1),
  ('Tap & Faucet Installation',  (SELECT id FROM categories WHERE category_type='Plumbing'),   1800.00, 'Install or replace taps, mixers and faucets',     1),
  ('Drain Unblocking',           (SELECT id FROM categories WHERE category_type='Plumbing'),   3000.00, 'Clear blocked drains, sinks and floor traps',     1),
  ('Water Heater Service',       (SELECT id FROM categories WHERE category_type='Plumbing'),   3500.00, 'Install and service electric/solar water heaters',1),
  ('Wiring & Rewiring',          (SELECT id FROM categories WHERE category_type='Electrical'), 3500.00, 'Full or partial house wiring, code-compliant',    1),
  ('Fan & Light Installation',   (SELECT id FROM categories WHERE category_type='Electrical'), 1500.00, 'Install ceiling fans and light fixtures',         1),
  ('Switchboard & Breaker Repair',(SELECT id FROM categories WHERE category_type='Electrical'),2800.00, 'Repair switchboards, breakers and fuses',         1),
  ('Power Outlet Installation',  (SELECT id FROM categories WHERE category_type='Electrical'), 1200.00, 'Install or relocate sockets and switches',        1),
  ('Deep House Cleaning',        (SELECT id FROM categories WHERE category_type='Cleaning'),   4000.00, 'Full-home deep clean, top to bottom',             1),
  ('Bathroom & Kitchen Cleaning',(SELECT id FROM categories WHERE category_type='Cleaning'),   2500.00, 'Detailed bathroom and kitchen sanitising',        1),
  ('Carpet & Sofa Cleaning',     (SELECT id FROM categories WHERE category_type='Cleaning'),   3000.00, 'Shampoo and steam clean carpets and sofas',       1),
  ('Post-Construction Cleaning', (SELECT id FROM categories WHERE category_type='Cleaning'),   6000.00, 'Debris and dust clearance after building work',   1),
  ('Wall Plastering',            (SELECT id FROM categories WHERE category_type='Masonry'),    4500.00, 'Interior and exterior wall plastering',           1),
  ('Tile Laying',                (SELECT id FROM categories WHERE category_type='Masonry'),    5000.00, 'Floor and wall tiling, indoor and outdoor',       1),
  ('Brick & Block Work',         (SELECT id FROM categories WHERE category_type='Masonry'),    5500.00, 'Brick walls, block work and boundary walls',      1),
  ('Concrete Repair',            (SELECT id FROM categories WHERE category_type='Masonry'),    4000.00, 'Repair cracks, slabs and concrete surfaces',      1),
  ('Furniture Assembly',         (SELECT id FROM categories WHERE category_type='Carpentry'),  2000.00, 'Assemble flat-pack and modular furniture',        1),
  ('Door & Window Fitting',      (SELECT id FROM categories WHERE category_type='Carpentry'),  3000.00, 'Fit and repair doors, frames and windows',        1),
  ('Custom Cabinets',            (SELECT id FROM categories WHERE category_type='Carpentry'),  8000.00, 'Design and build custom cabinets and wardrobes',  1),
  ('Wood Repair & Polishing',    (SELECT id FROM categories WHERE category_type='Carpentry'),  2500.00, 'Repair, sand and polish wooden surfaces',         1);

-- ---------- 3) Provider accounts (verified) ----------
-- user_type SERVICE_PROVIDER, is_verified=1 and service_category matching a category
-- name -> shown as display name; leads carry the TOP_RATED badge.
INSERT IGNORE INTO users
  (user_type, name, email, username, password_hash, phone, address, is_active, is_blacklisted,
   is_verified, service_category, availability_status, rating, badge_level, access_level, created_at, updated_at)
VALUES
  ('SERVICE_PROVIDER','Mr. AB Perera',        'abperera@fixitnow.lk',       'ab.perera',       @ph,'+94 77 123 4501','Colombo',   1,0,1,'Plumbing',  'available',4.9,'TOP_RATED',1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Nimal Fernando',   'nimal.fernando@fixitnow.lk', 'nimal.fernando',  @ph,'+94 76 220 1132','Gampaha',   1,0,1,'Plumbing',  'available',4.6,'SILVER',   1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Kasun Silva',      'kasun.silva@fixitnow.lk',    'kasun.silva',     @ph,'+94 71 884 7720','Kalutara',  1,0,1,'Plumbing',  'available',4.4,'BRONZE',   1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Dayapaala',        'dayapaala@fixitnow.lk',      'dayapaala',       @ph,'+94 77 905 3360','Gampaha',   1,0,1,'Electrical','available',4.8,'TOP_RATED',1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Saman Jayasuriya', 'saman.jaya@fixitnow.lk',     'saman.jaya',      @ph,'+94 70 112 9043','Colombo',   1,0,1,'Electrical','available',4.5,'SILVER',   1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Pradeep Kumara',   'pradeep.kumara@fixitnow.lk', 'pradeep.kumara',  @ph,'+94 76 451 8890','Kandy',     1,0,1,'Electrical','available',4.3,'BRONZE',   1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Sunimal Withana',  'sunimal@fixitnow.lk',        'sunimal.withana', @ph,'+94 77 332 1180','Colombo',   1,0,1,'Cleaning',  'available',4.9,'TOP_RATED',1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Ms. Kumari Rajapaksa', 'kumari.raj@fixitnow.lk',     'kumari.raj',      @ph,'+94 71 660 2245','Dehiwala',  1,0,1,'Cleaning',  'available',4.7,'SILVER',   1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Janaka Bandara',   'janaka.bandara@fixitnow.lk', 'janaka.bandara',  @ph,'+94 76 778 9931','Maharagama',1,0,1,'Cleaning',  'available',4.4,'BRONZE',   1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Abeykoon',         'abeykoon@fixitnow.lk',       'abeykoon',        @ph,'+94 77 540 7781','Kandy',     1,0,1,'Masonry',   'available',4.8,'TOP_RATED',1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Wijesinghe',       'wijesinghe@fixitnow.lk',     'wijesinghe',      @ph,'+94 70 223 6650','Kurunegala',1,0,1,'Masonry',   'available',4.5,'SILVER',   1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Gunaratne',        'gunaratne@fixitnow.lk',      'gunaratne',       @ph,'+94 71 909 4412','Matale',    1,0,1,'Masonry',   'available',4.2,'BRONZE',   1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Ruwan',            'ruwan@fixitnow.lk',          'ruwan.carpenter', @ph,'+94 77 818 2204','Negombo',   1,0,1,'Carpentry', 'available',4.9,'TOP_RATED',1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Lasantha Perera',  'lasantha.perera@fixitnow.lk','lasantha.perera', @ph,'+94 76 119 5567','Gampaha',   1,0,1,'Carpentry', 'available',4.6,'SILVER',   1,NOW(),NOW()),
  ('SERVICE_PROVIDER','Mr. Mahinda Dias',     'mahinda.dias@fixitnow.lk',   'mahinda.dias',    @ph,'+94 70 664 3389','Galle',     1,0,1,'Carpentry', 'available',4.3,'BRONZE',   1,NOW(),NOW());

-- ---------- 4) Provider map locations ----------
INSERT IGNORE INTO provider_locations (provider_id, latitude, longitude, updated_at)
SELECT id,
       CASE address
         WHEN 'Colombo' THEN 6.9271 WHEN 'Gampaha' THEN 7.0840 WHEN 'Kalutara' THEN 6.5854
         WHEN 'Kandy' THEN 7.2906 WHEN 'Dehiwala' THEN 6.8510 WHEN 'Maharagama' THEN 6.8480
         WHEN 'Kurunegala' THEN 7.4863 WHEN 'Matale' THEN 7.4675 WHEN 'Negombo' THEN 7.2083
         WHEN 'Galle' THEN 6.0535 ELSE 6.9271 END,
       CASE address
         WHEN 'Colombo' THEN 79.8612 WHEN 'Gampaha' THEN 79.9990 WHEN 'Kalutara' THEN 79.9607
         WHEN 'Kandy' THEN 80.6337 WHEN 'Dehiwala' THEN 79.8650 WHEN 'Maharagama' THEN 79.9265
         WHEN 'Kurunegala' THEN 80.3623 WHEN 'Matale' THEN 80.6234 WHEN 'Negombo' THEN 79.8358
         WHEN 'Galle' THEN 80.2210 ELSE 79.8612 END,
       NOW()
FROM users
WHERE user_type='SERVICE_PROVIDER' AND email LIKE '%@fixitnow.lk';

-- ---------- 5) Provider service cards ("My Service Cards") ----------
-- Note: emoji is intentionally left NULL -- the live `emoji` column is utf8 (3-byte)
-- and rejects 4-byte emoji characters; the frontend supplies a default icon. To store
-- emojis, first: ALTER TABLE provider_service_cards MODIFY emoji VARCHAR(16) CHARACTER SET utf8mb4;
INSERT INTO provider_service_cards
  (provider_id, title, category, rate_type, rate_amount, min_fee, short_summary, full_description,
   duration, warranty, status, districts, bg, price_line, published, created_at, updated_at)
SELECT u.id,
       CONCAT(u.service_category, ' Services by ', u.name),
       u.service_category, 'hourly',
       CASE u.service_category WHEN 'Plumbing' THEN 2500 WHEN 'Electrical' THEN 3000 WHEN 'Cleaning' THEN 2000 WHEN 'Masonry' THEN 3500 WHEN 'Carpentry' THEN 2500 END,
       CASE u.service_category WHEN 'Plumbing' THEN 1500 WHEN 'Electrical' THEN 1800 WHEN 'Cleaning' THEN 2500 WHEN 'Masonry' THEN 4000 WHEN 'Carpentry' THEN 2000 END,
       CONCAT('Professional ', LOWER(u.service_category), ' work across ', u.address, ' and nearby areas.'),
       CONCAT('Experienced, verified ', LOWER(u.service_category), ' provider offering reliable service with quality workmanship. Based in ', u.address, '.'),
       '2-3 hours', '30-day workmanship warranty', 'published', u.address, 'bg-amber-100',
       CONCAT('From Rs ', FORMAT(CASE u.service_category WHEN 'Plumbing' THEN 2500 WHEN 'Electrical' THEN 3000 WHEN 'Cleaning' THEN 2000 WHEN 'Masonry' THEN 3500 WHEN 'Carpentry' THEN 2500 END, 0), '/hr'),
       1, NOW(), NOW()
FROM users u
WHERE u.user_type='SERVICE_PROVIDER' AND u.email LIKE '%@fixitnow.lk'
  AND NOT EXISTS (SELECT 1 FROM provider_service_cards c WHERE c.provider_id = u.id);
