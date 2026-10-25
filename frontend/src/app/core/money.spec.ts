import { currencyDecimals, formatMoney } from './money';

describe('money formatting', () => {
  it('uses 3 decimals for TND and 2 for EUR and USD', () => {
    expect(currencyDecimals('TND')).toBe(3);
    expect(currencyDecimals('EUR')).toBe(2);
    expect(currencyDecimals('USD')).toBe(2);
  });

  it('shows millimes for TND without rounding the amount sent by the API', () => {
    const text = formatMoney(50, 'TND', 'en');
    expect(text).toContain('50.000');
  });

  it('shows two decimals for EUR', () => {
    expect(formatMoney(14.71, 'EUR', 'en')).toContain('14.71');
    expect(formatMoney(29.4, 'EUR', 'en')).toContain('29.40');
  });

  it('formats an amount the same whatever the language, apart from separators', () => {
    const digits = (s: string) => s.replace(/\D/g, '');
    expect(digits(formatMoney(1234.5, 'USD', 'fr'))).toBe('123450');
    expect(digits(formatMoney(1234.5, 'USD', 'en'))).toBe('123450');
  });
});
