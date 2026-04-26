-- ============================================================
-- Customer Management System - DML (seed.sql)
-- ============================================================
USE customer_db;

-- -------------------------------------------------------
-- Countries (5)
-- -------------------------------------------------------
INSERT INTO countries (id, name) VALUES
(1, 'Sri Lanka'),
(2, 'India'),
(3, 'United States'),
(4, 'United Kingdom'),
(5, 'Australia');

-- -------------------------------------------------------
-- Cities (10)
-- -------------------------------------------------------
INSERT INTO cities (id, name, country_id) VALUES
(1,  'Colombo',        1),
(2,  'Kandy',          1),
(3,  'Galle',          1),
(4,  'Mumbai',         2),
(5,  'New Delhi',      2),
(6,  'New York',       3),
(7,  'Los Angeles',    3),
(8,  'London',         4),
(9,  'Manchester',     4),
(10, 'Sydney',         5);

-- -------------------------------------------------------
-- Sample Customers
-- -------------------------------------------------------
INSERT INTO customers (id, name, date_of_birth, nic_number, created_at) VALUES
(1, 'Alice Fernando',  '1990-05-15', '901234567V', NOW()),
(2, 'Bob Perera',      '1985-11-20', '851234567V', NOW()),
(3, 'Carol Silva',     '1992-03-08', '921234567V', NOW()),
(4, 'David Rajapaksa', '1978-07-30', '781234567V', NOW()),
(5, 'Eva Mendis',      '1995-01-12', '951234567V', NOW());

-- -------------------------------------------------------
-- Sample Phones
-- -------------------------------------------------------
INSERT INTO customer_phones (customer_id, phone_number) VALUES
(1, '+94771234567'),
(1, '+94112345678'),
(2, '+94779876543'),
(3, '+94712345678'),
(4, '+94701122334'),
(5, '+94765544332');

-- -------------------------------------------------------
-- Sample Addresses
-- -------------------------------------------------------
INSERT INTO addresses (customer_id, address_line1, address_line2, city_id) VALUES
(1, '10 Main Street',   'Colombo 03',   1),
(2, '25 Kandy Road',    NULL,           2),
(3, '7 Galle Fort',     'Old Town',     3),
(4, '100 Marine Drive', 'Mumbai South', 4),
(5, '55 George Street', NULL,           10);

-- -------------------------------------------------------
-- Sample Family Relationships
-- -------------------------------------------------------
INSERT INTO customer_family (customer_id, family_member_id) VALUES
(1, 2),
(2, 1),
(3, 4),
(4, 3);
