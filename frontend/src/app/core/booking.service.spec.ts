import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { BookingQuote, BookingService } from './booking.service';

describe('BookingService', () => {
  let service: BookingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(BookingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('asks the server for the price in the chosen currency, with the round-trip flag', () => {
    let result: BookingQuote | undefined;
    service
      .quote({ airportId: 1, zoneId: 2, vehicleTypeId: 3, currency: 'EUR', roundTrip: true })
      .subscribe((q) => (result = q));

    const req = http.expectOne((r) => r.url === '/api/public/booking-quote');
    expect(req.request.params.get('currency')).toBe('EUR');
    expect(req.request.params.get('roundTrip')).toBe('true');
    expect(req.request.params.get('airportId')).toBe('1');
    req.flush({ leg: { amount: 14.71, currency: 'EUR' }, legs: 2, totalAmount: 29.42 });

    // The total is the server's: the browser does not multiply or convert.
    expect(result?.totalAmount).toBe(29.42);
  });

  it('cancels through the API', () => {
    service.cancel(7).subscribe();
    const req = http.expectOne('/api/bookings/7/cancel');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('edits only the practical details and reads one booking', () => {
    service
      .update(7, { address: '1 Rue X', flightNumber: 'TU714', pickupAt: '2026-12-01T10:30:00.000Z', luggageCount: 2, notes: '' })
      .subscribe();
    const put = http.expectOne('/api/bookings/7');
    expect(put.request.method).toBe('PUT');
    expect(Object.keys(put.request.body as object).sort()).toEqual(
      ['address', 'flightNumber', 'luggageCount', 'notes', 'pickupAt']
    );
    put.flush({});

    service.get(7).subscribe();
    http.expectOne('/api/bookings/7').flush({});
  });
});
