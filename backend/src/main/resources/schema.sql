-- Customer Management System - DDL (schema.sql)

CREATE DATABASE IF NOT EXISTS customer_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE customer_db;

-- -------------------------------------------------------
-- 1. countries
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS countries (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------
-- 2. cities
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS cities (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    country_id BIGINT NOT NULL,
    CONSTRAINT fk_city_country FOREIGN KEY (country_id) REFERENCES countries(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_city_country_id ON cities(country_id);

-- -------------------------------------------------------
-- 3. customers
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    date_of_birth DATE         NOT NULL,
    nic_number    VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_nic UNIQUE (nic_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_customer_nic ON customers(nic_number);

-- -------------------------------------------------------
-- 4. customer_phones
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_phones (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id  BIGINT       NOT NULL,
    phone_number VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_phone_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_phone_customer_id ON customer_phones(customer_id);

-- -------------------------------------------------------
-- 5. addresses
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS addresses (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id   BIGINT       NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city_id       BIGINT       NOT NULL,
    CONSTRAINT fk_address_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_address_city     FOREIGN KEY (city_id)     REFERENCES cities(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_address_customer_id ON addresses(customer_id);
CREATE INDEX idx_address_city_id     ON addresses(city_id);

-- -------------------------------------------------------
-- 6. customer_family (self-referencing join table)
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_family (
    customer_id       BIGINT NOT NULL,
    family_member_id  BIGINT NOT NULL,
    PRIMARY KEY (customer_id, family_member_id),
    CONSTRAINT fk_family_customer FOREIGN KEY (customer_id)      REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_family_member   FOREIGN KEY (family_member_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_family_member_id ON customer_family(family_member_id);
