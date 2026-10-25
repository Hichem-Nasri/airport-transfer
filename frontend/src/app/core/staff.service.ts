import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Booking, BookingStatus } from './booking.service';

export interface Person {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
}

export interface HistoryEntry {
  fromStatus: BookingStatus | null;
  toStatus: BookingStatus;
  actorId: number | null;
  actorName: string | null;
  actorRole: string | null;
  note: string | null;
  createdAt: string;
}

/** A booking as staff see it; `history` is only present on the detail view. */
export interface StaffBooking {
  booking: Booking;
  customer: Person | null;
  driver: Person | null;
  history: HistoryEntry[] | null;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface StaffFilter {
  status?: BookingStatus | '';
  /** UTC instants: `from` inclusive, `to` exclusive. */
  from?: string;
  to?: string;
  airportId?: number | null;
  driverId?: number | null;
  unassigned?: boolean;
  q?: string;
  page: number;
  size: number;
  order: 'asc' | 'desc';
}

/** Booking management for moderators and admins (`/api/staff`). */
@Injectable({ providedIn: 'root' })
export class StaffService {
  private readonly http = inject(HttpClient);

  list(filter: StaffFilter): Observable<Page<StaffBooking>> {
    let params = new HttpParams().set('page', filter.page).set('size', filter.size).set('order', filter.order);
    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.from) {
      params = params.set('from', filter.from);
    }
    if (filter.to) {
      params = params.set('to', filter.to);
    }
    if (filter.airportId) {
      params = params.set('airportId', filter.airportId);
    }
    if (filter.driverId) {
      params = params.set('driverId', filter.driverId);
    }
    if (filter.unassigned) {
      params = params.set('unassigned', true);
    }
    if (filter.q?.trim()) {
      params = params.set('q', filter.q.trim());
    }
    return this.http.get<Page<StaffBooking>>('/api/staff/bookings', { params });
  }

  get(id: number): Observable<StaffBooking> {
    return this.http.get<StaffBooking>(`/api/staff/bookings/${id}`);
  }

  drivers(): Observable<Person[]> {
    return this.http.get<Person[]>('/api/staff/drivers');
  }

  confirm(id: number): Observable<StaffBooking> {
    return this.http.post<StaffBooking>(`/api/staff/bookings/${id}/confirm`, {});
  }

  assign(id: number, driverId: number, force = false): Observable<StaffBooking> {
    return this.http.post<StaffBooking>(`/api/staff/bookings/${id}/assign`, { driverId, force });
  }

  start(id: number): Observable<StaffBooking> {
    return this.http.post<StaffBooking>(`/api/staff/bookings/${id}/start`, {});
  }

  cashCollected(id: number): Observable<StaffBooking> {
    return this.http.post<StaffBooking>(`/api/staff/bookings/${id}/cash-collected`, {});
  }

  complete(id: number): Observable<StaffBooking> {
    return this.http.post<StaffBooking>(`/api/staff/bookings/${id}/complete`, {});
  }

  cancel(id: number, reason: string): Observable<StaffBooking> {
    return this.http.post<StaffBooking>(`/api/staff/bookings/${id}/cancel`, reason.trim() ? { reason: reason.trim() } : {});
  }
}
