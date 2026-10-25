import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { Localized } from './localized';

export interface Airport extends Localized {
  id: number;
  iataCode: string;
}

export interface Area extends Localized {
  id: number;
  code: string;
  zoneId: number;
}

export interface ZoneWithAreas extends Localized {
  id: number;
  code: string;
  areas: Area[];
}

export interface VehicleType extends Localized {
  id: number;
  code: string;
  minPassengers: number;
  maxPassengers: number;
}

/** Read-only public catalog, fetched once and cached. */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  private readonly airports$ = this.http.get<Airport[]>('/api/public/airports').pipe(shareReplay(1));
  private readonly zones$ = this.http.get<ZoneWithAreas[]>('/api/public/zones').pipe(shareReplay(1));
  private readonly vehicleTypes$ = this.http.get<VehicleType[]>('/api/public/vehicle-types').pipe(shareReplay(1));

  airports(): Observable<Airport[]> {
    return this.airports$;
  }

  zones(): Observable<ZoneWithAreas[]> {
    return this.zones$;
  }

  vehicleTypes(): Observable<VehicleType[]> {
    return this.vehicleTypes$;
  }
}
