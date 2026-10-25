import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DriverService } from './driver.service';

describe('DriverService', () => {
  it('records cash through the API and never sends an amount (the server uses the booking snapshot)', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(DriverService);
    const http = TestBed.inject(HttpTestingController);

    service.cashCollected(5).subscribe();
    const req = http.expectOne('/api/driver/trips/5/cash-collected');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({});
    http.verify();
  });
});
