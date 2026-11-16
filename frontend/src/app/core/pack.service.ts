import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BookingsCreated, Direction, PaymentMethod, Quote } from './booking.service';

export interface PackLegRequest {
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
}

export interface CreatePackRequest {
  legs: PackLegRequest[];
  currency: string;
  paymentMethod: PaymentMethod;
}

export interface PackQuoteLeg {
  airportId: number;
  zoneId: number;
  vehicleTypeId: number;
}

/** Price of every leg and their total, all computed by the server. */
export interface PackQuote {
  currency: string;
  legs: Quote[];
  totalAmount: number;
  demo: boolean;
}

@Injectable({ providedIn: 'root' })
export class PackService {
  private readonly http = inject(HttpClient);

  quote(currency: string, legs: PackQuoteLeg[]): Observable<PackQuote> {
    return this.http.post<PackQuote>('/api/public/pack-quote', { currency, legs });
  }

  create(request: CreatePackRequest): Observable<BookingsCreated> {
    return this.http.post<BookingsCreated>('/api/bookings/pack', request);
  }
}
