-- ============================================================================
-- DEMO DATA - NOT REAL PRICES - REPLACE WITH THE ACTUAL PRICE GRID
-- ----------------------------------------------------------------------------
-- Every tariff and exchange rate inserted here has is_demo = TRUE.
-- Demo price (TND) = band by approximate straight-line distance between the airport and the
-- zone's main city (<30 km: 50, <100 km: 100, <250 km: 200, otherwise 300),
-- times a vehicle multiplier (SEDAN x1, VAN x1.5, MINIBUS x2.5).
-- Demo exchange rates (TND for 1 unit): EUR 3.400000, USD 3.100000 - placeholders, not market rates.
-- When the real grid is known: delete the is_demo rows and load the real data (admin API or a migration).
-- ============================================================================

WITH centroids (zone_code, lat, lng) AS (VALUES
    ('Z01', 36.8065, 10.1815),
    ('Z02', 36.4000, 10.6150),
    ('Z03', 36.4029, 10.1429),
    ('Z04', 37.2744,  9.8739),
    ('Z05', 36.3700,  9.0100),
    ('Z06', 35.8256, 10.6369),
    ('Z07', 35.6781, 10.0963),
    ('Z08', 35.1000,  9.1600),
    ('Z09', 34.7406, 10.7603),
    ('Z10', 33.8815, 10.0982),
    ('Z11', 34.4250,  8.7842),
    ('Z12', 33.8100,  8.5500),
    ('Z13', 33.8754, 10.8577),
    ('Z14', 33.4500, 10.8000),
    ('Z15', 32.9297, 10.4518)
),
distances AS (
    SELECT a.id AS airport_id,
           z.id AS zone_id,
           2 * 6371 * asin(sqrt(
               power(sin(radians((c.lat - a.latitude)::double precision) / 2), 2)
             + cos(radians(a.latitude::double precision)) * cos(radians(c.lat::double precision))
             * power(sin(radians((c.lng - a.longitude)::double precision) / 2), 2)
           )) AS km
    FROM airports a
    CROSS JOIN centroids c
    JOIN zones z ON z.code = c.zone_code
),
bands AS (
    SELECT airport_id, zone_id,
           CASE WHEN km < 30  THEN 50
                WHEN km < 100 THEN 100
                WHEN km < 250 THEN 200
                ELSE 300 END AS base_tnd
    FROM distances
)
INSERT INTO tariffs (airport_id, zone_id, vehicle_type_id, price_tnd, is_demo)
SELECT b.airport_id,
       b.zone_id,
       vt.id,
       b.base_tnd * CASE vt.code WHEN 'SEDAN' THEN 1.0 WHEN 'VAN' THEN 1.5 WHEN 'MINIBUS' THEN 2.5 END,
       TRUE
FROM bands b
CROSS JOIN vehicle_types vt;

INSERT INTO exchange_rates (currency_code, tnd_per_unit, is_demo) VALUES
 ('EUR', 3.400000, TRUE),
 ('USD', 3.100000, TRUE);
