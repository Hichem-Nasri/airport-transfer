import { Lang } from './language.service';

export const TUNIS_ZONE = 'Africa/Tunis';

const LOCALES: Record<Lang, string> = { fr: 'fr-FR', en: 'en-GB', ar: 'ar-TN' };

interface WallClock {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
  second: number;
}

function wallClock(utcMs: number, timeZone: string): WallClock {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    hourCycle: 'h23',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).formatToParts(new Date(utcMs));
  const get = (type: string): number => Number(parts.find((p) => p.type === type)?.value ?? '0');
  return {
    year: get('year'),
    month: get('month'),
    day: get('day'),
    hour: get('hour'),
    minute: get('minute'),
    second: get('second')
  };
}

/** Offset of the zone from UTC, in milliseconds, at the given instant (daylight saving time included). */
function offsetMs(utcMs: number, timeZone: string): number {
  const w = wallClock(utcMs, timeZone);
  const asUtc = Date.UTC(w.year, w.month - 1, w.day, w.hour, w.minute, w.second);
  return asUtc - Math.floor(utcMs / 1000) * 1000;
}

const pad = (n: number): string => String(n).padStart(2, '0');

/** Converts a wall-clock date (yyyy-MM-dd) and time (HH:mm) in the given zone into the UTC instant the API expects. */
export function localToIso(date: string, time: string, timeZone: string): string {
  const [y, m, d] = date.split('-').map(Number);
  const [h, min] = time.split(':').map(Number);
  const wall = Date.UTC(y, m - 1, d, h, min);
  const firstGuess = wall - offsetMs(wall, timeZone);
  const utc = wall - offsetMs(firstGuess, timeZone);
  return new Date(utc).toISOString();
}

/** Splits a UTC instant into the zone's wall-clock date (yyyy-MM-dd) and time (HH:mm), for form fields. */
export function isoToLocalParts(iso: string, timeZone: string): { date: string; time: string } {
  const w = wallClock(Date.parse(iso), timeZone);
  return { date: `${w.year}-${pad(w.month)}-${pad(w.day)}`, time: `${pad(w.hour)}:${pad(w.minute)}` };
}

/** Today's date in the zone as yyyy-MM-dd (value for a date input's `min`). */
export function todayIn(timeZone: string, now: Date = new Date()): string {
  return isoToLocalParts(now.toISOString(), timeZone).date;
}

/** Shows a UTC instant in the zone, e.g. "1 déc. 2026, 11:30". */
export function formatInZone(iso: string, lang: Lang, timeZone: string): string {
  return new Intl.DateTimeFormat(LOCALES[lang], { timeZone, dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(iso)
  );
}

/** The UTC instant at which a calendar day (yyyy-MM-dd) of the zone starts. */
export function dayStartIso(date: string, timeZone: string): string {
  return localToIso(date, '00:00', timeZone);
}

/** The UTC instant at which the next calendar day of the zone starts (an exclusive end for a day filter). */
export function dayEndIso(date: string, timeZone: string): string {
  const [y, m, d] = date.split('-').map(Number);
  const next = new Date(Date.UTC(y, m - 1, d + 1)).toISOString().slice(0, 10);
  return localToIso(next, '00:00', timeZone);
}
