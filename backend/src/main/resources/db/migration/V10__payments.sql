-- Payment status on bookings and a ledger of payments. Money columns follow the bookings snapshot (amount in the
-- currency charged); nothing here ever recomputes a price.
ALTER TABLE bookings ADD COLUMN payment_status VARCHAR(10) NOT NULL DEFAULT 'UNPAID';
ALTER TABLE bookings ADD COLUMN paid_at TIMESTAMPTZ;
ALTER TABLE bookings ADD CONSTRAINT ck_bookings_payment_status CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED'));
CREATE INDEX ix_bookings_payment_status ON bookings (payment_status, status);

CREATE TABLE payments (
    id           BIGSERIAL     PRIMARY KEY,
    booking_id   BIGINT        NOT NULL REFERENCES bookings (id),
    provider     VARCHAR(20)   NOT NULL,
    amount       NUMERIC(12,3) NOT NULL CHECK (amount > 0),
    currency     CHAR(3)       NOT NULL,
    status       VARCHAR(10)   NOT NULL,
    external_ref VARCHAR(100),
    recorded_by  BIGINT        REFERENCES users (id),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    refunded_at  TIMESTAMPTZ,
    CONSTRAINT ck_payments_status CHECK (status IN ('SUCCEEDED', 'REFUNDED'))
);
CREATE INDEX ix_payments_booking ON payments (booking_id);
-- At most one successful (not refunded) payment per booking.
CREATE UNIQUE INDEX ux_payments_one_success ON payments (booking_id) WHERE status = 'SUCCEEDED';
