-- Step 11b: transfers in France. Airports and zones belong to a country, an airport has its own time zone,
-- tariffs are stored in the currency they are sold in (EUR in France) and vehicle types can list example models.

ALTER TABLE airports ADD COLUMN country VARCHAR(2) NOT NULL DEFAULT 'TN';
ALTER TABLE airports ADD COLUMN time_zone VARCHAR(40) NOT NULL DEFAULT 'Africa/Tunis';
ALTER TABLE zones ADD COLUMN country VARCHAR(2) NOT NULL DEFAULT 'TN';
ALTER TABLE vehicle_types ADD COLUMN models VARCHAR(200);

-- Tariffs were all in TND: the existing rows keep that currency.
ALTER TABLE tariffs RENAME COLUMN price_tnd TO price;
ALTER TABLE tariffs ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'TND' REFERENCES currencies (code);
COMMENT ON COLUMN tariffs.price IS 'Price in the tariff currency (column currency), not necessarily TND.';
