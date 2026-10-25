-- Accounts. Public sign-up only creates CUSTOMER; DRIVER and ADMIN accounts are created by an admin.
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'DRIVER', 'ADMIN'))
);

-- E-mail is unique regardless of case.
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
