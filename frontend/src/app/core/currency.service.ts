import { Injectable, signal } from '@angular/core';

export type Currency = 'TND' | 'EUR' | 'USD';

@Injectable({ providedIn: 'root' })
export class CurrencyService {
  readonly available: Currency[] = ['TND', 'EUR', 'USD'];
  readonly current = signal<Currency>('TND');

  set(currency: Currency): void {
    this.current.set(currency);
  }
}
