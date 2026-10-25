-- Booking operations: MODERATOR role, status history, customer language, optimistic locking.
ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'DRIVER', 'MODERATOR', 'ADMIN'));

ALTER TABLE bookings ADD COLUMN customer_language VARCHAR(2) NOT NULL DEFAULT 'fr';
ALTER TABLE bookings ADD CONSTRAINT ck_bookings_language CHECK (customer_language IN ('fr', 'en', 'ar'));
ALTER TABLE bookings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX ix_bookings_driver ON bookings (assigned_driver_id, pickup_at) WHERE assigned_driver_id IS NOT NULL;

-- One row per status change (and per driver change: from_status = to_status).
CREATE TABLE booking_status_history (
    id          BIGSERIAL    PRIMARY KEY,
    booking_id  BIGINT       NOT NULL REFERENCES bookings (id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20)  NOT NULL,
    actor_id    BIGINT       REFERENCES users (id),
    actor_role  VARCHAR(20),
    note        VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_history_from CHECK (from_status IS NULL OR from_status IN
        ('PENDING', 'CONFIRMED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_history_to CHECK (to_status IN
        ('PENDING', 'CONFIRMED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX ix_history_booking ON booking_status_history (booking_id, created_at, id);

-- Existing bookings get their creation as first history row.
INSERT INTO booking_status_history (booking_id, from_status, to_status, actor_id, actor_role, created_at)
SELECT id, NULL, 'PENDING', customer_id, 'CUSTOMER', created_at FROM bookings;
