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

  it('accepts today', () => {
    expect(parsePrefill(convertToParamMap({ date: today }), today).date).toBe(today);
  });
});
