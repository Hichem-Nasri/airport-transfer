import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Localized } from './localized';

export type Direction = 'AIRPORT_TO_ADDRESS' | 'ADDRESS_TO_AIRPORT';
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type PaymentMethod = 'CASH' | 'CARD';
export type PaymentStatus = 'UNPAID' | 'PAID' | 'REFUNDED';

export interface Quote {
  airportId: number;
  zoneId: number;
  vehicleTypeId: number;
  priceTnd: number;
  currency: string;
  amount: number;
  exchangeRate: number | null;
  demo: boolean;
}

/** Price of one leg, number of legs and total: all computed by the server. */
export interface BookingQuote {
  leg: Quote;
  legs: number;
  totalAmount: number;
}

export interface QuoteParams {
  airportId: number;
  zoneId: number;
  vehicleTypeId: number;
  currency: string;
  roundTrip: boolean;
}

export interface CreateBookingRequest {
  airportId: number;
  direction: Direction;
  zoneId: number;
  address: string;
  flightNumber?: string;
  pickupAt: string;
  passengers: number;
  luggageCount: number;
  vehicleTypeId: number;
  notes?: string;
  currency: string;
  paymentMethod: PaymentMethod;
  returnPickupAt?: string;
  returnFlightNumber?: string;
}

export interface Booking {
  id: number;
  reference: string;
  groupId: string | null;
  direction: Direction;
  status: BookingStatus;
  airport: Localized & { id: number; iataCode: string };
  zone: Localized & { id: number; code: string };
  address: string;
  flightNumber: string | null;
  pickupAt: string;
  passengers: number;
  luggageCount: number;
  vehicleType: Localized & { id: number; code: string; maxPassengers: number };
  notes: string | null;
  priceTnd: number;
  currency: string;
  exchangeRate: number | null;
  amount: number;
  priceDemo: boolean;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  cancellable: boolean;
  editable: boolean;
  createdAt: string;
}

/** The practical details a customer may change while a booking is PENDING or CONFIRMED. */
export interface UpdateBookingRequest {
  address: string;
  flightNumber: string;
  pickupAt: string;
  luggageCount: number;
  notes: string;
}

export interface BookingsCreated {
  groupId: string | null;
  bookings: Booking[];
}

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);

  quote(params: QuoteParams): Observable<BookingQuote> {
    const query = new HttpParams()
      .set('airportId', params.airportId)
      .set('zoneId', params.zoneId)
      .set('vehicleTypeId', params.vehicleTypeId)
      .set('currency', params.currency)
      .set('roundTrip', params.roundTrip);
    return this.http.get<BookingQuote>('/api/public/booking-quote', { params: query });
  }

  create(request: CreateBookingRequest): Observable<BookingsCreated> {
    return this.http.post<BookingsCreated>('/api/bookings', request);
  }

  mine(): Observable<Booking[]> {
    return this.http.get<Booking[]>('/api/bookings');
  }

  get(id: number): Observable<Booking> {
    return this.http.get<Booking>(`/api/bookings/${id}`);
  }

  update(id: number, request: UpdateBookingRequest): Observable<Booking> {
    return this.http.put<Booking>(`/api/bookings/${id}`, request);
  }

  cancel(id: number): Observable<Booking> {
    return this.http.post<Booking>(`/api/bookings/${id}/cancel`, {});
  }
}
