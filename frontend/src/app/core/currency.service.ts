import { Injectable, signal } from '@angular/core';

export type Currency = 'TND' | 'EUR' | 'USD';

@Injectable({ providedIn: 'root' })
export class CurrencyService {
  readonly available: Currency[] = ['EUR', 'USD', 'TND'];
  readonly current = signal<Currency>('EUR');

  set(currency: Currency): void {
    this.current.set(currency);
  }
}
