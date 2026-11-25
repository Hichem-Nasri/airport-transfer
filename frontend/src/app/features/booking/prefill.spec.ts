import { convertToParamMap } from '@angular/router';
import { parsePrefill } from './prefill';

describe('parsePrefill', () => {
  const today = '2026-12-01';

  it('reads valid values', () => {
    const p = parsePrefill(
      convertToParamMap({
        direction: 'ADDRESS_TO_AIRPORT', airportId: '3', areaId: '12', date: '2026-12-05', time: '09:30', passengers: '4'
      }),
      today
    );
    expect(p).toEqual({
      direction: 'ADDRESS_TO_AIRPORT', airportId: 3, areaId: 12, date: '2026-12-05', time: '09:30', passengers: 4
    });
  });

  it('ignores malformed values, past dates and out-of-range passengers', () => {
    const p = parsePrefill(
      convertToParamMap({
        direction: 'SIDEWAYS', airportId: '-1', areaId: 'abc', date: '2026-11-30', time: '25:00', passengers: '500'
      }),
      today
    );
    expect(Object.values(p).every((v) => v === undefined)).toBeTrue();
  });

  it('reads the round trip, address and luggage handed over by the card', () => {
    const p = parsePrefill(
      convertToParamMap({ roundTrip: '1', returnDate: '2026-12-09', returnTime: '18:15', address: ' Hôtel Marhaba, Sousse ', luggage: '3' }),
      today
    );
    expect(p.roundTrip).toBeTrue();
    expect(p.returnDate).toBe('2026-12-09');
    expect(p.returnTime).toBe('18:15');
    expect(p.address).toBe('Hôtel Marhaba, Sousse');
    expect(p.luggage).toBe(3);
  });

  it('ignores a malformed return, luggage and an empty address', () => {
    const p = parsePrefill(convertToParamMap({ roundTrip: '1', returnDate: '2026-11-01', luggage: '900', address: '  ' }), today);
    expect(p.returnDate).toBeUndefined();
    expect(p.luggage).toBeUndefined();
    expect(p.address).toBeUndefined();
  });

  it('accepts today', () => {
    expect(parsePrefill(convertToParamMap({ date: today }), today).date).toBe(today);
  });
});
