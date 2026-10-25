import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { AdminService, Stats } from '../../core/admin.service';
import { errorKey } from '../../core/api-error';
import { BookingStatus } from '../../core/booking.service';
import { LanguageService } from '../../core/language.service';
import { formatMoney } from '../../core/money';
import { formatTunis, tunisDayEndIso, tunisDayStartIso } from '../../core/tunis-time';

const STATUSES: BookingStatus[] = ['PENDING', 'CONFIRMED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

@Component({
  selector: 'app-admin-stats',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="load()" class="grid gap-3 rounded-2xl border border-brand-line bg-white p-4 sm:grid-cols-3">
      <div>
        <label class="field-label" for="from">{{ 'staff.filters.from' | translate }}</label>
        <input id="from" type="date" class="field-input" formControlName="from" />
      </div>
      <div>
        <label class="field-label" for="to">{{ 'staff.filters.to' | translate }}</label>
        <input id="to" type="date" class="field-input" formControlName="to" />
      </div>
      <div class="flex items-end gap-2">
        <button type="button" class="btn-secondary" (click)="reset()">{{ 'staff.filters.reset' | translate }}</button>
        <button type="submit" class="btn-primary">{{ 'staff.filters.apply' | translate }}</button>
      </div>
    </form>

    @if (error(); as key) {
      <p class="mt-6 rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
    }

    @if (loading()) {
      <p class="mt-6 text-muted" role="status">{{ 'account.loading' | translate }}</p>
    } @else {
    @if (stats(); as s) {
      <p class="mt-4 text-sm text-muted" role="status">
        {{ 'admin.stats.period' | translate: { from: when(s.from), to: when(s.to) } }}
      </p>

      <div class="mt-4 grid gap-3 sm:grid-cols-2">
        <div class="rounded-2xl border border-brand-line bg-white p-5">
          <p class="text-sm font-semibold text-muted">{{ 'admin.stats.total' | translate }}</p>
          <p class="mt-1 text-4xl font-extrabold">{{ s.totalBookings }}</p>
        </div>
        <div class="rounded-2xl border border-brand-line bg-white p-5">
          <p class="text-sm font-semibold text-muted">{{ 'admin.stats.active' | translate }}</p>
          <p class="mt-1 text-4xl font-extrabold">{{ s.activeBookings }}</p>
        </div>
      </div>

      <h2 class="mt-8 text-xl font-extrabold">{{ 'admin.stats.byStatus' | translate }}</h2>
      <ul class="mt-3 space-y-2">
        @for (st of statuses; track st) {
          <li class="rounded-xl border border-brand-line bg-white p-3">
            <div class="flex justify-between text-sm font-semibold">
              <span>{{ 'status.' + st | translate }}</span>
              <span>{{ s.byStatus[st] }}</span>
            </div>
            <div class="mt-2 h-2 rounded-full bg-brand-soft" aria-hidden="true">
              <div class="h-2 rounded-full bg-brand" [style.width.%]="share(s, s.byStatus[st])"></div>
            </div>
          </li>
        }
      </ul>

      <h2 class="mt-8 text-xl font-extrabold">{{ 'admin.stats.revenue' | translate }}</h2>
      <p class="mt-1 text-sm text-muted">{{ 'admin.stats.revenueHint' | translate }}</p>
      @if (s.revenue.length === 0) {
        <p class="mt-3 rounded-2xl border border-brand-line bg-white p-4 font-semibold">{{ 'admin.stats.none' | translate }}</p>
      } @else {
        <div class="mt-3 overflow-x-auto rounded-2xl border border-brand-line bg-white">
          <table class="w-full text-start text-sm">
            <thead>
              <tr class="border-b border-brand-line text-muted">
                <th scope="col" class="p-3 text-start">{{ 'admin.stats.currency' | translate }}</th>
                <th scope="col" class="p-3 text-start">{{ 'admin.stats.bookings' | translate }}</th>
                <th scope="col" class="p-3 text-start">{{ 'admin.stats.charged' | translate }}</th>
                <th scope="col" class="p-3 text-start">{{ 'admin.stats.collected' | translate }}</th>
                <th scope="col" class="p-3 text-start">{{ 'admin.stats.baseTnd' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (r of s.revenue; track r.currency) {
                <tr class="border-b border-brand-line last:border-0">
                  <th scope="row" class="p-3 text-start font-extrabold">{{ r.currency }}</th>
                  <td class="p-3">{{ r.bookings }}</td>
                  <td class="p-3 font-extrabold">{{ money(r.amount, r.currency) }}</td>
                  <td class="p-3">{{ money(r.collected, r.currency) }}</td>
                  <td class="p-3">{{ money(r.amountTnd, 'TND') }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      <h2 class="mt-8 text-xl font-extrabold">{{ 'admin.stats.byAirport' | translate }}</h2>
      <ul class="mt-3 grid gap-2 sm:grid-cols-2">
        @for (a of s.byAirport; track a.airportId) {
          <li class="flex justify-between rounded-xl border border-brand-line bg-white p-3 font-semibold">
            <span>{{ a.iataCode }}</span><span>{{ a.bookings }}</span>
          </li>
        }
      </ul>
    }
    }
  `
})
export class AdminStatsComponent {
  private readonly admin = inject(AdminService);
  private readonly lang = inject(LanguageService);

  readonly statuses = STATUSES;
  readonly form = inject(FormBuilder).nonNullable.group({ from: [''], to: [''] });
  readonly stats = signal<Stats | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  reset(): void {
    this.form.reset();
    this.load();
  }

  load(): void {
    const v = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.admin.stats(v.from ? tunisDayStartIso(v.from) : undefined, v.to ? tunisDayEndIso(v.to) : undefined).subscribe({
      next: (s) => {
        this.stats.set(s);
        this.loading.set(false);
      },
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }

  /** Width of a status bar relative to the total (a display ratio, not money). */
  share(s: Stats, count: number): number {
    return s.totalBookings === 0 ? 0 : Math.round((count / s.totalBookings) * 100);
  }

  when(iso: string): string {
    return formatTunis(iso, this.lang.current());
  }

  money(amount: number, currency: string): string {
    return formatMoney(amount, currency, this.lang.current());
  }
}
