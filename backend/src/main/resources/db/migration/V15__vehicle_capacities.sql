-- Client request: Berline and Eco carry up to 4 passengers, Van up to 8.
UPDATE vehicle_types SET min_passengers = 1, max_passengers = 4 WHERE code IN ('SEDAN', 'ECO', 'ECO_PLUS');
UPDATE vehicle_types SET min_passengers = 5, max_passengers = 8 WHERE code = 'VAN';
