-- SMS reminders before pickup: one marker per reminder so each is sent at most once.
ALTER TABLE bookings ADD COLUMN reminder_24h_sent_at TIMESTAMPTZ;
ALTER TABLE bookings ADD COLUMN reminder_1h_sent_at  TIMESTAMPTZ;
