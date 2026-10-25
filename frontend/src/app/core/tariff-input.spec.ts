import { parseTndInput } from './tariff-input';

describe('parseTndInput', () => {
  it('accepts whole numbers, dot and comma decimals up to 3 digits', () => {
    expect(parseTndInput('45')).toBe(45);
    expect(parseTndInput(' 45,5 ')).toBe(45.5);
    expect(parseTndInput('45.125')).toBe(45.125);
  });

  it('rejects empty, zero, negative, too precise or non numeric values', () => {
    for (const bad of ['', '0', '0.000', '-5', '1.2345', 'abc', '1e3', '12345678', '1,2,3']) {
      expect(parseTndInput(bad)).withContext(bad).toBeNull();
    }
  });
});

import { parseRateInput } from './tariff-input';

describe('parseRateInput', () => {
  it('accepts up to 6 decimals', () => {
    expect(parseRateInput('3,4')).toBe(3.4);
    expect(parseRateInput('3.412345')).toBe(3.412345);
  });

  it('rejects zero, too precise or malformed values', () => {
    for (const bad of ['', '0', '3.1234567', '-1', 'x']) {
      expect(parseRateInput(bad)).withContext(bad).toBeNull();
    }
  });
});
