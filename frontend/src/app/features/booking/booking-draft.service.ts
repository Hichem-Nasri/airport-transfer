import { Injectable, signal } from '@angular/core';
import { BookingsCreated, Direction } from '../../core/booking.service';

const DRAFT_KEY = 'hms.book.draft';

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
  /** IANA time zone of the airport: the date and time fields above are wall-clock times in it. */
  timeZone: string;
  /** UTC instants, ISO-8601. */
  pickupAt: string;
  returnPickupAt: string | null;
  terminal?: string;
  address?: string;
  luggageCount?: number;
}

/** State shared by the three steps. Provided by BookingFlowComponent, so it lives as long as the flow. */
@Injectable()
export class BookingDraftService {
  readonly step = signal<Step>(1);
  readonly trip = signal<TripDraft | null>(null);
  readonly vehicleTypeId = signal<number | null>(null);
  readonly created = signal<BookingsCreated | null>(null);
  /** Details typed before the visitor was sent to create an account; applied once by the details step. */
  readonly pendingDetails = signal<Record<string, unknown> | null>(null);

  constructor() {
    this.restore();
  }

  /** Keeps the booking while an anonymous visitor creates an account, then comes back to confirm. */
  persist(details: Record<string, unknown>): void {
    try {
      sessionStorage.setItem(
        DRAFT_KEY,
        JSON.stringify({ trip: this.trip(), vehicleTypeId: this.vehicleTypeId(), details })
      );
    } catch {
      // storage unavailable: the visitor fills the booking again
    }
  }

  private restore(): void {
    try {
      const raw = sessionStorage.getItem(DRAFT_KEY);
      if (!raw) {
        return;
      }
      sessionStorage.removeItem(DRAFT_KEY);
      const saved = JSON.parse(raw) as { trip: TripDraft | null; vehicleTypeId: number | null; details: Record<string, unknown> };
      if (saved.trip && saved.vehicleTypeId !== null) {
        this.trip.set(saved.trip);
        this.vehicleTypeId.set(saved.vehicleTypeId);
        this.pendingDetails.set(saved.details ?? null);
        this.step.set(3);
      }
    } catch {
      // unreadable draft: ignore it
    }
  }

  go(step: Step): void {
    this.step.set(step);
    window.scrollTo({ top: 0 });
  }
}
