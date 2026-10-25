import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { StaffService } from './staff.service';

describe('StaffService', () => {
  let service: StaffService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(StaffService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends only the filters that are set', () => {
    service.list({ page: 1, size: 20, order: 'asc', status: '', airportId: null, q: '  ', unassigned: false }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/staff/bookings');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('order')).toBe('asc');
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.has('airportId')).toBeFalse();
    expect(req.request.params.has('q')).toBeFalse();
    expect(req.request.params.has('unassigned')).toBeFalse();
    req.flush({ content: [], page: 1, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('sends status, search, unassigned and the date range', () => {
    service
      .list({
        page: 0, size: 20, order: 'desc', status: 'CONFIRMED', q: ' MH-AB ', unassigned: true, driverId: 4,
        from: '2026-12-01T23:00:00.000Z', to: '2026-12-02T23:00:00.000Z'
      })
      .subscribe();

    const req = http.expectOne((r) => r.url === '/api/staff/bookings');
    expect(req.request.params.get('status')).toBe('CONFIRMED');
    expect(req.request.params.get('q')).toBe('MH-AB');
    expect(req.request.params.get('unassigned')).toBe('true');
    expect(req.request.params.get('driverId')).toBe('4');
    expect(req.request.params.get('from')).toBe('2026-12-01T23:00:00.000Z');
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('assigns with the force flag only when asked', () => {
    service.assign(9, 3).subscribe();
    http.expectOne('/api/staff/bookings/9/assign').flush({});
    service.assign(9, 3, true).subscribe();
    const req = http.expectOne('/api/staff/bookings/9/assign');
    expect(req.request.body).toEqual({ driverId: 3, force: true });
    req.flush({});
  });

  it('cancels with a trimmed reason, or without a body when there is none', () => {
    service.cancel(5, '  No driver  ').subscribe();
    const withReason = http.expectOne('/api/staff/bookings/5/cancel');
    expect(withReason.request.body).toEqual({ reason: 'No driver' });
    withReason.flush({});

    service.cancel(5, '   ').subscribe();
    const without = http.expectOne('/api/staff/bookings/5/cancel');
    expect(without.request.body).toEqual({});
    without.flush({});
  });
});
