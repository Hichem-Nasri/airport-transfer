import { dayEndIso, dayStartIso, formatInZone, isoToLocalParts, localToIso, todayIn } from './zoned-time';

const PARIS = 'Europe/Paris';
const TUNIS = 'Africa/Tunis';

describe('Zoned time', () => {
  it('converts Paris wall-clock time with winter (UTC+1) and summer (UTC+2) offsets', () => {
    expect(localToIso('2026-12-01', '11:30', PARIS)).toBe('2026-12-01T10:30:00.000Z');
    expect(localToIso('2026-07-15', '11:30', PARIS)).toBe('2026-07-15T09:30:00.000Z');
  });

  it('keeps Tunis at UTC+1 all year', () => {
    expect(localToIso('2026-12-01', '11:30', TUNIS)).toBe('2026-12-01T10:30:00.000Z');
    expect(localToIso('2026-07-15', '11:30', TUNIS)).toBe('2026-07-15T10:30:00.000Z');
  });

  it('handles the days around the Paris clock change', () => {
    expect(localToIso('2026-03-28', '12:00', PARIS)).toBe('2026-03-28T11:00:00.000Z');
    expect(localToIso('2026-03-29', '12:00', PARIS)).toBe('2026-03-29T10:00:00.000Z');
    expect(localToIso('2026-10-24', '12:00', PARIS)).toBe('2026-10-24T10:00:00.000Z');
    expect(localToIso('2026-10-25', '12:00', PARIS)).toBe('2026-10-25T11:00:00.000Z');
  });

  it('rolls over midnight', () => {
    expect(localToIso('2026-07-15', '00:30', PARIS)).toBe('2026-07-14T22:30:00.000Z');
  });

  it('splits an instant and round-trips with localToIso', () => {
    expect(isoToLocalParts('2026-07-15T09:30:00Z', PARIS)).toEqual({ date: '2026-07-15', time: '11:30' });
    expect(isoToLocalParts('2026-11-30T23:30:00Z', TUNIS)).toEqual({ date: '2026-12-01', time: '00:30' });
    const parts = isoToLocalParts('2026-07-15T08:05:00Z', PARIS);
    expect(localToIso(parts.date, parts.time, PARIS)).toBe('2026-07-15T08:05:00.000Z');
  });

  it('computes today in the zone, not in UTC', () => {
    expect(todayIn(PARIS, new Date('2026-07-15T22:30:00Z'))).toBe('2026-07-16');
    expect(todayIn(TUNIS, new Date('2026-07-15T22:30:00Z'))).toBe('2026-07-15');
  });

  it('turns a calendar day into a half-open UTC range', () => {
    expect(dayStartIso('2026-07-16', PARIS)).toBe('2026-07-15T22:00:00.000Z');
    expect(dayEndIso('2026-07-16', PARIS)).toBe('2026-07-16T22:00:00.000Z');
    expect(dayEndIso('2026-03-28', PARIS)).toBe('2026-03-28T23:00:00.000Z');
  });

  it('shows an instant in the zone whatever the browser time zone', () => {
    expect(formatInZone('2026-07-15T09:30:00Z', 'en', PARIS)).toContain('11:30');
    expect(formatInZone('2026-07-15T09:30:00Z', 'en', TUNIS)).toContain('10:30');
  });
});
