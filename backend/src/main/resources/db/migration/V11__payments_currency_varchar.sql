-- V10 declared payments.currency as CHAR(3); every other currency column is VARCHAR(3) and Hibernate validates
-- the entity against VARCHAR. Values are always 3 letters, so the conversion loses nothing.
ALTER TABLE payments ALTER COLUMN currency TYPE VARCHAR(3);
