import { formatTunis, isoToTunisParts, todayInTunis, tunisDayEndIso, tunisDayStartIso, tunisLocalToIso } from './tunis-time';

describe('Tunis time', () => {
  it('converts a Tunis wall-clock time to UTC (UTC+1, no daylight saving time)', () => {
    expect(tunisLocalToIso('2026-12-01', '11:30')).toBe('2026-12-01T10:30:00.000Z');
    expect(tunisLocalToIso('2026-07-15', '11:30')).toBe('2026-07-15T10:30:00.000Z');
  });

  it('rolls over midnight correctly', () => {
    expect(tunisLocalToIso('2026-12-01', '00:30')).toBe('2026-11-30T23:30:00.000Z');
  });

  it('shows a UTC instant in Tunis time whatever the browser time zone', () => {
    expect(formatTunis('2026-12-01T10:30:00Z', 'en')).toContain('11:30');
  });

  it('computes today in Tunis, not in UTC', () => {
    // 23:30 UTC is already the next day in Tunis.
    expect(todayInTunis(new Date('2026-12-01T23:30:00Z'))).toBe('2026-12-02');
    expect(todayInTunis(new Date('2026-12-01T10:00:00Z'))).toBe('2026-12-01');
  });

  it('turns a Tunis calendar day into a half-open UTC range', () => {
    expect(tunisDayStartIso('2026-12-02')).toBe('2026-12-01T23:00:00.000Z');
    expect(tunisDayEndIso('2026-12-02')).toBe('2026-12-02T23:00:00.000Z');
  });

  it('splits an instant into Tunis date and time and round-trips with tunisLocalToIso', () => {
    expect(isoToTunisParts('2026-12-01T10:30:00Z')).toEqual({ date: '2026-12-01', time: '11:30' });
    expect(isoToTunisParts('2026-11-30T23:30:00Z')).toEqual({ date: '2026-12-01', time: '00:30' });
    const parts = isoToTunisParts('2026-07-15T08:05:00Z');
    expect(tunisLocalToIso(parts.date, parts.time)).toBe('2026-07-15T08:05:00.000Z');
  });
});
