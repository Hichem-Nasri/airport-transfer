import { Injectable, signal } from '@angular/core';
import { BookingsCreated, Direction } from '../../core/booking.service';

export type Step = 1 | 2 | 3;

/** What step 1 collects. The raw date and time fields are kept so the form can be restored on "back". */
export interface TripDraft {
  direction: Direction;
  roundTrip: boolean;
  airportId: number;
  areaId: number;
  zoneId: number;
  date: string;
  time: string;
  returnDate: string;
  returnTime: string;
  passengers: number;
  /** UTC instants, ISO-8601. */
  pickupAt: string;
  returnPickupAt: string | null;
}

/** State shared by the three steps. Provided by BookingFlowComponent, so it lives as long as the flow. */
@Injectable()
export class BookingDraftService {
  readonly step = signal<Step>(1);
  readonly trip = signal<TripDraft | null>(null);
  readonly vehicleTypeId = signal<number | null>(null);
  readonly created = signal<BookingsCreated | null>(null);

  go(step: Step): void {
    this.step.set(step);
    window.scrollTo({ top: 0 });
  }
}
