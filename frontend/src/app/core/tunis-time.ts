import { Lang } from './language.service';
import { TUNIS_ZONE, dayEndIso, dayStartIso, formatInZone, isoToLocalParts, localToIso, todayIn } from './zoned-time';

/** Tunis-time shortcuts, used by the admin screens. Bookings use the zoned functions with their airport's zone. */
export function tunisLocalToIso(date: string, time: string): string {
  return localToIso(date, time, TUNIS_ZONE);
}

export function isoToTunisParts(iso: string): { date: string; time: string } {
  return isoToLocalParts(iso, TUNIS_ZONE);
}

export function todayInTunis(now: Date = new Date()): string {
  return todayIn(TUNIS_ZONE, now);
}

export function formatTunis(iso: string, lang: Lang): string {
  return formatInZone(iso, lang, TUNIS_ZONE);
}

export function tunisDayStartIso(date: string): string {
  return dayStartIso(date, TUNIS_ZONE);
}

export function tunisDayEndIso(date: string): string {
  return dayEndIso(date, TUNIS_ZONE);
}
