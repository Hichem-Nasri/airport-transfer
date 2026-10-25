import { Lang } from './language.service';

const LOCALES: Record<Lang, string> = { fr: 'fr-FR', en: 'en-GB', ar: 'ar-TN' };
const TUNIS = 'Africa/Tunis';

/** Tunisia is UTC+1 all year (no daylight saving time since 2009). */
const TUNIS_OFFSET = '+01:00';

/** Converts a Tunis wall-clock date (yyyy-MM-dd) and time (HH:mm) into the UTC instant the API expects. */
export function tunisLocalToIso(date: string, time: string): string {
  return new Date(`${date}T${time}:00${TUNIS_OFFSET}`).toISOString();
}

/** Splits a UTC instant into the Tunis wall-clock date (yyyy-MM-dd) and time (HH:mm), for form fields. */
export function isoToTunisParts(iso: string): { date: string; time: string } {
  const shifted = new Date(Date.parse(iso) + 60 * 60 * 1000).toISOString();
  return { date: shifted.slice(0, 10), time: shifted.slice(11, 16) };
}

/** Today's date in Tunis as yyyy-MM-dd (value for a date input's `min`). */
export function todayInTunis(now: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: TUNIS,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(now);
}

/** Shows a UTC instant in Tunis time, e.g. "1 déc. 2026, 11:30". */
export function formatTunis(iso: string, lang: Lang): string {
  return new Intl.DateTimeFormat(LOCALES[lang], {
    timeZone: TUNIS,
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(iso));
}

/** The UTC instant at which a Tunis calendar day (yyyy-MM-dd) starts. */
export function tunisDayStartIso(date: string): string {
  return tunisLocalToIso(date, '00:00');
}

/** The UTC instant at which the next Tunis calendar day starts (an exclusive end for a day filter). */
export function tunisDayEndIso(date: string): string {
  return new Date(Date.parse(tunisDayStartIso(date)) + 24 * 60 * 60 * 1000).toISOString();
}
