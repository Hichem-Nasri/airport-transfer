-- Bookings. Every booking carries its own price snapshot: later tariff or exchange-rate changes never alter it.
-- price_tnd is the base price in TND, currency/exchange_rate the currency chosen and the rate used
-- (TND per 1 unit, NULL for TND), amount the final charged amount in that currency.
CREATE TABLE bookings (
    id                 BIGSERIAL     PRIMARY KEY,
    reference          VARCHAR(20)   NOT NULL,
    group_id           UUID,
    customer_id        BIGINT        NOT NULL REFERENCES users (id),
    airport_id         BIGINT        NOT NULL REFERENCES airports (id),
    direction          VARCHAR(20)   NOT NULL,
    zone_id            BIGINT        NOT NULL REFERENCES zones (id),
    address            VARCHAR(500)  NOT NULL,
    flight_number      VARCHAR(10),
    pickup_at          TIMESTAMPTZ   NOT NULL,
    passengers         INTEGER       NOT NULL,
    luggage_count      INTEGER       NOT NULL DEFAULT 0,
    vehicle_type_id    BIGINT        NOT NULL REFERENCES vehicle_types (id),
    notes              VARCHAR(1000),
    price_tnd          NUMERIC(10,3) NOT NULL,
    currency           VARCHAR(3)    NOT NULL REFERENCES currencies (code),
    exchange_rate      NUMERIC(14,6),
    amount             NUMERIC(12,3) NOT NULL,
    price_is_demo      BOOLEAN       NOT NULL DEFAULT FALSE,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_method     VARCHAR(10)   NOT NULL,
    assigned_driver_id BIGINT        REFERENCES users (id),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_bookings_reference UNIQUE (reference),
    CONSTRAINT ck_bookings_direction CHECK (direction IN ('AIRPORT_TO_ADDRESS', 'ADDRESS_TO_AIRPORT')),
    CONSTRAINT ck_bookings_status CHECK (status IN
        ('PENDING', 'CONFIRMED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_bookings_payment_method CHECK (payment_method IN ('CASH', 'CARD')),
    CONSTRAINT ck_bookings_passengers CHECK (passengers >= 1),
    CONSTRAINT ck_bookings_luggage CHECK (luggage_count >= 0),
    CONSTRAINT ck_bookings_price CHECK (price_tnd > 0 AND amount > 0),
    CONSTRAINT ck_bookings_rate CHECK (exchange_rate IS NULL OR exchange_rate > 0)
);

CREATE INDEX ix_bookings_customer ON bookings (customer_id, pickup_at DESC);
CREATE INDEX ix_bookings_group ON bookings (group_id) WHERE group_id IS NOT NULL;
CREATE INDEX ix_bookings_status_pickup ON bookings (status, pickup_at);
