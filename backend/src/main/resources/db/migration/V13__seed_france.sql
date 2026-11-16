-- Step 11b: Paris airports, the Paris flat-rate zone, the Eco / Eco Plus categories and the client's prices
-- (EUR 150 Eco, EUR 160 Eco Plus, per trip). These are real prices given by the client, not demo data.

INSERT INTO airports (iata_code, name_fr, name_en, name_ar, country, time_zone, latitude, longitude) VALUES
 ('ORY', 'Paris-Orly',             'Paris-Orly',             'باريس أورلي',       'FR', 'Europe/Paris', 48.723300, 2.379400),
 ('CDG', 'Paris-Charles de Gaulle', 'Paris-Charles de Gaulle', 'باريس شارل ديغول', 'FR', 'Europe/Paris', 49.009700, 2.547900);

INSERT INTO zones (code, name_fr, name_en, name_ar, country, display_order) VALUES
 ('FR01', 'Paris', 'Paris', 'باريس', 'FR', 20);

INSERT INTO areas (code, name_fr, name_en, name_ar, zone_id)
SELECT 'FR-PARIS', 'Paris', 'Paris', 'باريس', id FROM zones WHERE code = 'FR01';

INSERT INTO vehicle_types (code, name_fr, name_en, name_ar, models, min_passengers, max_passengers, display_order) VALUES
 ('ECO',      'Eco',      'Eco',      'اقتصادي',      'Seat Ibiza, Toyota Yaris, Renault Clio', 1, 3, 4),
 ('ECO_PLUS', 'Eco Plus', 'Eco Plus', 'اقتصادي بلاس', 'Toyota C-HR, Toyota Corolla',            1, 3, 5);

INSERT INTO tariffs (airport_id, zone_id, vehicle_type_id, price, currency, is_demo)
SELECT a.id, z.id, v.id, CASE v.code WHEN 'ECO' THEN 150 ELSE 160 END, 'EUR', FALSE
FROM airports a, zones z, vehicle_types v
WHERE a.iata_code IN ('ORY', 'CDG') AND z.code = 'FR01' AND v.code IN ('ECO', 'ECO_PLUS');
