ALTER TABLE airports ADD COLUMN terminals VARCHAR(200);
ALTER TABLE bookings ADD COLUMN terminal VARCHAR(60);

UPDATE airports SET terminals = 'Terminal 1,Terminal 2,Terminal 3' WHERE iata_code = 'CDG';
UPDATE airports SET terminals = 'Orly 1,Orly 2,Orly 3,Orly 4' WHERE iata_code = 'ORY';
