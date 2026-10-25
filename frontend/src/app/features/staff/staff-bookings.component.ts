import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { errorKey } from '../../core/api-error';
import { BookingStatus } from '../../core/booking.service';
import { Airport, CatalogService } from '../../core/catalog.service';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { formatMoney } from '../../core/money';
import { Page, Person, StaffBooking, StaffService } from '../../core/staff.service';
import { formatTunis, tunisDayEndIso, tunisDayStartIso } from '../../core/tunis-time';
import { StatusBadgeComponent } from '../../shared/status-badge.component';

const STATUSES: BookingStatus[] = ['PENDING', 'CONFIRMED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
const PAGE_SIZE = 20;

@Component({
  selector: 'app-staff-bookings',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslateModule, StatusBadgeComponent],
  template: `
    <section class="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6">
      <h1 class="text-3xl font-extrabold">{{ 'staff.title' | translate }}</h1>

      <form [formGroup]="form" (ngSubmit)="search()" class="mt-6 grid gap-3 rounded-2xl border border-brand-line bg-white p-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="sm:col-span-2">
          <label class="field-label" for="q">{{ 'staff.filters.search' | translate }}</label>
          <input id="q" type="search" class="field-input" formControlName="q" [placeholder]="'staff.filters.searchHint' | translate" />
        </div>
        <div>
          <label class="field-label" for="status">{{ 'staff.filters.status' | translate }}</label>
          <select id="status" class="field-input" formControlName="status">
            <option value="">{{ 'staff.filters.all' | translate }}</option>
            @for (s of statuses; track s) {
              <option [value]="s">{{ 'status.' + s | translate }}</option>
            }
          </select>
        </div>
        <div>
          <label class="field-label" for="airportId">{{ 'booking.fields.airport' | translate }}</label>
          <select id="airportId" class="field-input" formControlName="airportId">
            <option [ngValue]="null">{{ 'staff.filters.all' | translate }}</option>
            @for (a of airports(); track a.id) {
              <option [ngValue]="a.id">{{ a.iataCode }} — {{ name(a) }}</option>
            }
          </select>
        </div>
        <div>
          <label class="field-label" for="from">{{ 'staff.filters.from' | translate }}</label>
          <input id="from" type="date" class="field-input" formControlName="from" />
        </div>
        <div>
          <label class="field-label" for="to">{{ 'staff.filters.to' | translate }}</label>
          <input id="to" type="date" class="field-input" formControlName="to" />
        </div>
        <div>
          <label class="field-label" for="driverId">{{ 'staff.driver' | translate }}</label>
          <select id="driverId" class="field-input" formControlName="driverId">
            <option [ngValue]="null">{{ 'staff.filters.all' | translate }}</option>
            @for (d of drivers(); track d.id) {
              <option [ngValue]="d.id">{{ d.fullName }}</option>
            }
          </select>
        </div>
        <div>
          <label class="field-label" for="order">{{ 'staff.filters.order' | translate }}</label>
          <select id="order" class="field-input" formControlName="order">
            <option value="asc">{{ 'staff.filters.soonest' | translate }}</option>
            <option value="desc">{{ 'staff.filters.latest' | translate }}</option>
          </select>
        </div>
        <label class="flex min-h-11 items-center gap-3 font-semibold sm:col-span-2">
          <input type="checkbox" class="h-5 w-5 accent-[#D2001A]" formControlName="unassigned" />
          {{ 'staff.filters.unassigned' | translate }}
        </label>
        <div class="flex gap-2 sm:col-span-2 lg:justify-end">
          <button type="button" class="btn-secondary" (click)="reset()">{{ 'staff.filters.reset' | translate }}</button>
          <button type="submit" class="btn-primary">{{ 'staff.filters.apply' | translate }}</button>
        </div>
      </form>

      @if (error(); as key) {
        <p class="mt-6 rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
      }

      @if (loading()) {
        <p class="mt-8 text-muted" role="status">{{ 'account.loading' | translate }}</p>
      } @else {
      @if (page(); as p) {
        <p class="mt-6 text-sm font-semibold text-muted" role="status">
          {{ 'staff.count' | translate: { count: p.totalElements } }}
        </p>
        @if (p.content.length === 0) {
          <p class="mt-4 rounded-2xl border border-brand-line bg-white p-6 text-center font-semibold">{{ 'staff.empty' | translate }}</p>
        }
        <ul class="mt-4 space-y-3">
          @for (row of p.content; track row.booking.id) {
            <li>
              <a [routerLink]="['/staff/bookings', row.booking.id]"
                 class="block rounded-2xl border border-brand-line bg-white p-4 hover:border-brand focus:outline-none focus-visible:ring-2 focus-visible:ring-brand">
                <span class="flex flex-wrap items-center justify-between gap-2">
                  <span class="text-lg font-extrabold tracking-wide" dir="ltr">{{ row.booking.reference }}</span>
                  <app-status-badge [status]="row.booking.status" />
                </span>
                <span class="mt-2 flex flex-wrap items-center gap-x-2 font-semibold">
                  <span>{{ from(row) }}</span>
                  <span aria-hidden="true" class="text-brand rtl:rotate-180">→</span>
                  <span>{{ to(row) }}</span>
                </span>
                <span class="mt-1 block text-sm text-muted">{{ when(row.booking.pickupAt) }}</span>
                <span class="mt-3 grid gap-x-4 gap-y-1 text-sm sm:grid-cols-3">
                  <span><span class="text-muted">{{ 'staff.customer' | translate }} : </span><strong>{{ row.customer?.fullName }}</strong></span>
                  <span>
                    <span class="text-muted">{{ 'staff.driver' | translate }} : </span>
                    @if (row.driver) { <strong>{{ row.driver.fullName }}</strong> } @else { <em class="text-brand-dark">{{ 'staff.noDriver' | translate }}</em> }
                  </span>
                  <span class="font-extrabold">{{ money(row) }}</span>
                </span>
              </a>
            </li>
          }
        </ul>
        @if (p.totalPages > 1) {
          <nav class="mt-6 flex items-center justify-between gap-3" [attr.aria-label]="'staff.pagination' | translate">
            <button type="button" class="btn-secondary" [disabled]="p.page === 0" (click)="go(p.page - 1)">‹ {{ 'staff.previous' | translate }}</button>
            <span class="text-sm font-semibold">{{ p.page + 1 }} / {{ p.totalPages }}</span>
            <button type="button" class="btn-secondary" [disabled]="p.page + 1 >= p.totalPages" (click)="go(p.page + 1)">{{ 'staff.next' | translate }} ›</button>
          </nav>
        }
      }
      }
    </section>
  `
})
export class StaffBookingsComponent {
  private readonly staff = inject(StaffService);
  private readonly catalog = inject(CatalogService);
  private readonly lang = inject(LanguageService);

  readonly statuses = STATUSES;
  readonly airports = toSignal(this.catalog.airports(), { initialValue: [] as Airport[] });
  readonly drivers = toSignal(this.staff.drivers(), { initialValue: [] as Person[] });

  readonly form = inject(FormBuilder).nonNullable.group({
    q: [''],
    status: ['' as BookingStatus | ''],
    airportId: [null as number | null],
    driverId: [null as number | null],
    from: [''],
    to: [''],
    unassigned: [false],
    order: ['asc' as 'asc' | 'desc']
  });

  readonly page = signal<Page<StaffBooking> | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  private readonly pageIndex = signal(0);

  constructor() {
    this.load();
  }

  search(): void {
    this.go(0);
  }

  reset(): void {
    this.form.reset();
    this.go(0);
  }

  go(index: number): void {
    this.pageIndex.set(index);
    this.load();
  }

  name(a: Airport): string {
    return localName(a, this.lang.current());
  }

  from(row: StaffBooking): string {
    const b = row.booking;
    return b.direction === 'AIRPORT_TO_ADDRESS' ? b.airport.iataCode : localName(b.zone, this.lang.current());
  }

  to(row: StaffBooking): string {
    const b = row.booking;
    return b.direction === 'AIRPORT_TO_ADDRESS' ? localName(b.zone, this.lang.current()) : b.airport.iataCode;
  }

  when(iso: string): string {
    return formatTunis(iso, this.lang.current());
  }

  money(row: StaffBooking): string {
    return formatMoney(row.booking.amount, row.booking.currency, this.lang.current());
  }

  private load(): void {
    const v = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.staff
      .list({
        status: v.status,
        airportId: v.airportId,
        driverId: v.driverId,
        unassigned: v.unassigned,
        q: v.q,
        from: v.from ? tunisDayStartIso(v.from) : undefined,
        to: v.to ? tunisDayEndIso(v.to) : undefined,
        page: this.pageIndex(),
        size: PAGE_SIZE,
        order: v.order
      })
      .subscribe({
        next: (page) => {
          this.page.set(page);
          this.loading.set(false);
        },
        error: (e: unknown) => {
          this.error.set(errorKey(e));
          this.loading.set(false);
        }
      });
  }
}
