/**
 * Parses a TND price typed by an admin ("45", "45,5", "45.125"): at most 7 integer digits and 3 decimals
 * (millimes), strictly positive. Returns null when invalid. No arithmetic is done on money here.
 */
export function parseTndInput(text: string): number | null {
  const normalized = text.trim().replace(',', '.');
  if (!/^\d{1,7}(\.\d{1,3})?$/.test(normalized)) {
    return null;
  }
  const value = Number(normalized);
  return value > 0 ? value : null;
}

/**
 * Parses an exchange rate typed by an admin (TND per 1 unit of the currency): strictly positive,
 * at most 8 integer digits and 6 decimals, matching the API validation. Returns null when invalid.
 */
export function parseRateInput(text: string): number | null {
  const normalized = text.trim().replace(',', '.');
  if (!/^\d{1,8}(\.\d{1,6})?$/.test(normalized)) {
    return null;
  }
  const value = Number(normalized);
  return value > 0 ? value : null;
}
