import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Booking } from './booking.service';
import { Person } from './staff.service';

export interface DriverTrip {
  booking: Booking;
  customer: Person | null;
}

/** The signed-in driver's own trips (`/api/driver/trips`). */
@Injectable({ providedIn: 'root' })
export class DriverService {
  private readonly http = inject(HttpClient);

  trips(scope: 'upcoming' | 'past'): Observable<DriverTrip[]> {
    return this.http.get<DriverTrip[]>('/api/driver/trips', { params: { scope } });
  }

  start(id: number): Observable<DriverTrip> {
    return this.http.post<DriverTrip>(`/api/driver/trips/${id}/start`, {});
  }

  /** The driver confirms the customer paid in cash (the server checks the trip has started and is not paid yet). */
  cashCollected(id: number): Observable<DriverTrip> {
    return this.http.post<DriverTrip>(`/api/driver/trips/${id}/cash-collected`, {});
  }

  complete(id: number): Observable<DriverTrip> {
    return this.http.post<DriverTrip>(`/api/driver/trips/${id}/complete`, {});
  }
}
