import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BookingStatus } from './booking.service';
import { Role } from './auth.service';
import { Localized } from './localized';
import { Page } from './staff.service';

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface UserCreate {
  email: string;
  password: string;
  fullName: string;
  phone: string;
  role: Role;
}

export interface UserUpdate {
  fullName: string;
  phone: string;
  role: Role;
  enabled: boolean;
}

export interface UserFilter {
  q?: string;
  role?: Role | '';
  enabled?: boolean | null;
  page: number;
  size: number;
}

export interface AdminAirport extends Localized {
  id: number;
  iataCode: string;
  latitude: number | null;
  longitude: number | null;
  active: boolean;
}

export interface AdminZone extends Localized {
  id: number;
  code: string;
  displayOrder: number;
  active: boolean;
}

export interface AdminArea extends Localized {
  id: number;
  code: string;
  zoneId: number;
  active: boolean;
}

export interface AdminVehicleType extends Localized {
  id: number;
  code: string;
  minPassengers: number;
  maxPassengers: number;
  displayOrder: number;
  active: boolean;
}

export interface AdminCurrency {
  code: string;
  name: string;
  symbol: string;
  decimals: number;
  active: boolean;
}

export interface AdminRate {
  id: number;
  currencyCode: string;
  tndPerUnit: number;
  demo: boolean;
  createdAt: string;
}

export interface AdminTariff {
  id: number;
  airportId: number;
  zoneId: number;
  vehicleTypeId: number;
  priceTnd: number;
  active: boolean;
  demo: boolean;
}

export interface TariffWrite {
  airportId: number;
  zoneId: number;
  vehicleTypeId: number;
  priceTnd: number;
  active: boolean;
}

export interface CurrencyRevenue {
  currency: string;
  bookings: number;
  amount: number;
  amountTnd: number;
}

export interface AirportCount {
  airportId: number;
  iataCode: string;
  bookings: number;
}

export interface Stats {
  from: string;
  to: string;
  totalBookings: number;
  activeBookings: number;
  byStatus: Record<BookingStatus, number>;
  revenue: CurrencyRevenue[];
  byAirport: AirportCount[];
}

/** Administration API (`/api/admin`, ADMIN only). Catalog items are never deleted from the UI: they are deactivated. */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  // ---- users
  users(filter: UserFilter): Observable<Page<AdminUser>> {
    let params = new HttpParams().set('page', filter.page).set('size', filter.size);
    if (filter.q?.trim()) {
      params = params.set('q', filter.q.trim());
    }
    if (filter.role) {
      params = params.set('role', filter.role);
    }
    if (filter.enabled !== null && filter.enabled !== undefined) {
      params = params.set('enabled', filter.enabled);
    }
    return this.http.get<Page<AdminUser>>('/api/admin/users', { params });
  }

  createUser(body: UserCreate): Observable<AdminUser> {
    return this.http.post<AdminUser>('/api/admin/users', { ...body, phone: body.phone.trim() || null });
  }

  updateUser(id: number, body: UserUpdate): Observable<AdminUser> {
    return this.http.put<AdminUser>(`/api/admin/users/${id}`, { ...body, phone: body.phone.trim() || null });
  }

  // ---- statistics
  stats(from?: string, to?: string): Observable<Stats> {
    let params = new HttpParams();
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    return this.http.get<Stats>('/api/admin/stats', { params });
  }

  // ---- catalog
  airports(): Observable<AdminAirport[]> {
    return this.http.get<AdminAirport[]>('/api/admin/airports');
  }
  saveAirport(id: number | null, body: Omit<AdminAirport, 'id'>): Observable<AdminAirport> {
    return id === null
      ? this.http.post<AdminAirport>('/api/admin/airports', body)
      : this.http.put<AdminAirport>(`/api/admin/airports/${id}`, body);
  }

  zones(): Observable<AdminZone[]> {
    return this.http.get<AdminZone[]>('/api/admin/zones');
  }
  saveZone(id: number | null, body: Omit<AdminZone, 'id'>): Observable<AdminZone> {
    return id === null
      ? this.http.post<AdminZone>('/api/admin/zones', body)
      : this.http.put<AdminZone>(`/api/admin/zones/${id}`, body);
  }

  areas(): Observable<AdminArea[]> {
    return this.http.get<AdminArea[]>('/api/admin/areas');
  }
  saveArea(id: number | null, body: Omit<AdminArea, 'id'>): Observable<AdminArea> {
    return id === null
      ? this.http.post<AdminArea>('/api/admin/areas', body)
      : this.http.put<AdminArea>(`/api/admin/areas/${id}`, body);
  }

  vehicleTypes(): Observable<AdminVehicleType[]> {
    return this.http.get<AdminVehicleType[]>('/api/admin/vehicle-types');
  }
  saveVehicleType(id: number | null, body: Omit<AdminVehicleType, 'id'>): Observable<AdminVehicleType> {
    return id === null
      ? this.http.post<AdminVehicleType>('/api/admin/vehicle-types', body)
      : this.http.put<AdminVehicleType>(`/api/admin/vehicle-types/${id}`, body);
  }

  // ---- pricing
  currencies(): Observable<AdminCurrency[]> {
    return this.http.get<AdminCurrency[]>('/api/admin/currencies');
  }
  saveCurrency(existing: boolean, body: AdminCurrency): Observable<AdminCurrency> {
    return existing
      ? this.http.put<AdminCurrency>(`/api/admin/currencies/${body.code}`, body)
      : this.http.post<AdminCurrency>('/api/admin/currencies', body);
  }

  rates(currency?: string): Observable<AdminRate[]> {
    const params = currency ? new HttpParams().set('currency', currency) : new HttpParams();
    return this.http.get<AdminRate[]>('/api/admin/exchange-rates', { params });
  }
  addRate(currencyCode: string, tndPerUnit: number): Observable<AdminRate> {
    return this.http.post<AdminRate>('/api/admin/exchange-rates', { currencyCode, tndPerUnit });
  }

  tariffs(airportId: number): Observable<AdminTariff[]> {
    return this.http.get<AdminTariff[]>('/api/admin/tariffs', { params: new HttpParams().set('airportId', airportId) });
  }
  createTariff(body: TariffWrite): Observable<AdminTariff> {
    return this.http.post<AdminTariff>('/api/admin/tariffs', body);
  }
  updateTariff(id: number, body: TariffWrite): Observable<AdminTariff> {
    return this.http.put<AdminTariff>(`/api/admin/tariffs/${id}`, body);
  }
}
