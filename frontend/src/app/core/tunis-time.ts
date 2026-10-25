import { Lang } from './language.service';

const LOCALES: Record<Lang, string> = { fr: 'fr-FR', en: 'en-GB', ar: 'ar-TN' };
const TUNIS = 'Africa/Tunis';

/** Tunisia is UTC+1 all year (no daylight saving time since 2009). */
const TUNIS_OFFSET = '+01:00';

/** Converts a Tunis wall-clock date (yyyy-MM-dd) and time (HH:mm) into the UTC instant the API expects. */
export function tunisLocalToIso(date: string, time: string): string {
  return new Date(`${date}T${time}:00${TUNIS_OFFSET}`).toISOString();
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
