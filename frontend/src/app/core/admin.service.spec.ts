import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminService } from './admin.service';

describe('AdminService', () => {
  let service: AdminService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends only the user filters that are set', () => {
    service.users({ page: 0, size: 20, q: '  ', role: '', enabled: null }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/admin/users');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('q')).toBeFalse();
    expect(req.request.params.has('role')).toBeFalse();
    expect(req.request.params.has('enabled')).toBeFalse();
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('keeps enabled=false as a filter and sends a blank phone as null', () => {
    service.users({ page: 0, size: 20, role: 'DRIVER', enabled: false }).subscribe();
    const list = http.expectOne((r) => r.url === '/api/admin/users');
    expect(list.request.params.get('enabled')).toBe('false');
    expect(list.request.params.get('role')).toBe('DRIVER');
    list.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    service.updateUser(7, { fullName: 'A', phone: '  ', role: 'DRIVER', enabled: true }).subscribe();
    const put = http.expectOne('/api/admin/users/7');
    expect(put.request.method).toBe('PUT');
    expect((put.request.body as { phone: string | null }).phone).toBeNull();
    put.flush({});
  });

  it('creates a tariff when none exists and posts exchange rates as TND per unit', () => {
    service.createTariff({ airportId: 1, zoneId: 2, vehicleTypeId: 3, priceTnd: 45.5, active: true }).subscribe();
    const t = http.expectOne('/api/admin/tariffs');
    expect(t.request.method).toBe('POST');
    expect((t.request.body as { priceTnd: number }).priceTnd).toBe(45.5);
    t.flush({});

    service.addRate('EUR', 3.4125).subscribe();
    const r = http.expectOne('/api/admin/exchange-rates');
    expect(r.request.body).toEqual({ currencyCode: 'EUR', tndPerUnit: 3.4125 });
    r.flush({});
  });
});
