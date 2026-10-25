import { ParamMap } from '@angular/router';
import { Direction } from '../../core/booking.service';

export interface TripPrefill {
  direction?: Direction;
  airportId?: number;
  areaId?: number;
  date?: string;
  time?: string;
  passengers?: number;
}

function positiveInt(value: string | null): number | undefined {
  return value !== null && /^\d{1,9}$/.test(value) && Number(value) > 0 ? Number(value) : undefined;
}

/**
 * Reads the booking card's hand-off from the URL (`/book?airportId=..`). Anything malformed is ignored, and a
 * date in the past is dropped, so the form never starts with an invalid value coming from a stale link.
 */
export function parsePrefill(params: ParamMap, todayInTunis: string): TripPrefill {
  const result: TripPrefill = {};
  const direction = params.get('direction');
  if (direction === 'AIRPORT_TO_ADDRESS' || direction === 'ADDRESS_TO_AIRPORT') {
    result.direction = direction;
  }
  result.airportId = positiveInt(params.get('airportId'));
  result.areaId = positiveInt(params.get('areaId'));
  const date = params.get('date');
  if (date !== null && /^\d{4}-\d{2}-\d{2}$/.test(date) && date >= todayInTunis) {
    result.date = date;
  }
  const time = params.get('time');
  if (time !== null && /^([01]\d|2[0-3]):[0-5]\d$/.test(time)) {
    result.time = time;
  }
  const passengers = positiveInt(params.get('passengers'));
  if (passengers !== undefined && passengers <= 100) {
    result.passengers = passengers;
  }
  return result;
}
