import { formatTunis, todayInTunis, tunisLocalToIso } from './tunis-time';

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
});
