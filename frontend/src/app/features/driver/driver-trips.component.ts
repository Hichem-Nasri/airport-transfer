import { Component, inject, signal } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { errorKey } from '../../core/api-error';
import { DriverService, DriverTrip } from '../../core/driver.service';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { formatMoney } from '../../core/money';
import { formatTunis } from '../../core/tunis-time';
import { StatusBadgeComponent } from '../../shared/status-badge.component';

@Component({
  selector: 'app-driver-trips',
  standalone: true,
  imports: [TranslateModule, StatusBadgeComponent],
  template: `
    <section class="mx-auto w-full max-w-2xl px-4 py-8 sm:px-6">
      <div class="flex items-center justify-between gap-3">
        <h1 class="text-3xl font-extrabold">{{ 'driver.title' | translate }}</h1>
        <button type="button" class="btn-secondary" (click)="load()">{{ 'driver.refresh' | translate }}</button>
      </div>

      <div role="tablist" class="mt-6 grid grid-cols-2 gap-2 rounded-2xl border border-brand-line bg-white p-1.5">
        @for (s of scopes; track s) {
          <button type="button" role="tab" class="min-h-11 rounded-xl px-3 text-sm font-semibold"
                  [class.bg-brand]="scope() === s" [class.text-white]="scope() === s"
                  [attr.aria-selected]="scope() === s" (click)="select(s)">
            {{ 'driver.' + s | translate }}
          </button>
        }
      </div>

      @if (error(); as key) {
        <p class="mt-6 rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
      }

      @if (loading()) {
        <p class="mt-8 text-muted" role="status">{{ 'account.loading' | translate }}</p>
      } @else if (trips().length === 0 && !error()) {
        <p class="mt-8 rounded-2xl border border-brand-line bg-white p-6 text-center font-semibold">{{ 'driver.empty' | translate }}</p>
      } @else {
        <ul class="mt-6 space-y-4">
          @for (t of trips(); track t.booking.id) {
            <li class="rounded-2xl border border-brand-line bg-white p-4">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <p class="text-lg font-extrabold" dir="ltr">{{ when(t.booking.pickupAt) }}</p>
                <app-status-badge [status]="t.booking.status" />
              </div>
              <p class="mt-2 text-sm font-semibold text-brand">{{ 'booking.direction.' + t.booking.direction | translate }}</p>
              <p class="font-semibold">{{ t.booking.airport.iataCode }} · {{ name(t.booking.airport) }}</p>
              <dl class="mt-3 space-y-1 text-sm">
                <div><dt class="inline text-muted">{{ 'booking.fields.address' | translate }} : </dt><dd class="inline font-semibold">{{ name(t.booking.zone) }} — {{ t.booking.address }}</dd></div>
                @if (t.booking.flightNumber) {
                  <div><dt class="inline text-muted">{{ 'booking.fields.flightNumber' | translate }} : </dt><dd class="inline font-semibold" dir="ltr">{{ t.booking.flightNumber }}</dd></div>
                }
                <div><dt class="inline text-muted">{{ 'booking.fields.passengers' | translate }} / {{ 'booking.fields.luggage' | translate }} : </dt><dd class="inline font-semibold">{{ t.booking.passengers }} / {{ t.booking.luggageCount }}</dd></div>
                @if (t.booking.notes) {
                  <div><dt class="inline text-muted">{{ 'booking.fields.notes' | translate }} : </dt><dd class="inline font-semibold">{{ t.booking.notes }}</dd></div>
                }
              </dl>
              @if (t.customer; as c) {
                <p class="mt-3 font-semibold">{{ c.fullName }}</p>
                @if (c.phone) {
                  <a class="btn-secondary mt-1" dir="ltr" [href]="'tel:' + c.phone">📞 {{ c.phone }}</a>
                }
              }
              <p class="mt-3 text-lg font-extrabold">
                {{ 'driver.collect' | translate }} {{ money(t) }}
              </p>
              <p class="text-sm text-muted">
                {{ 'booking.payment.' + t.booking.paymentMethod | translate }} ·
                <span class="font-semibold" [class.text-brand-dark]="t.booking.paymentStatus === 'UNPAID'">{{ 'payment.status.' + t.booking.paymentStatus | translate }}</span>
              </p>
              @if (canCollect(t)) {
                <button type="button" class="btn-secondary mt-3 w-full" [disabled]="busy() === t.booking.id" (click)="run(t, service.cashCollected(t.booking.id))">
                  {{ 'driver.cashCollected' | translate }}
                </button>
              }

              @if (t.booking.status === 'ASSIGNED') {
                <button type="button" class="btn-primary mt-3 w-full" [disabled]="busy() === t.booking.id" (click)="run(t, service.start(t.booking.id))">
                  {{ 'driver.start' | translate }}
                </button>
              }
              @if (t.booking.status === 'IN_PROGRESS') {
                <button type="button" class="btn-primary mt-3 w-full" [disabled]="busy() === t.booking.id" (click)="run(t, service.complete(t.booking.id))">
                  {{ 'driver.complete' | translate }}
                </button>
              }
            </li>
          }
        </ul>
      }
    </section>
  `
})
export class DriverTripsComponent {
  readonly service = inject(DriverService);
  private readonly lang = inject(LanguageService);

  readonly scopes: ('upcoming' | 'past')[] = ['upcoming', 'past'];
  readonly scope = signal<'upcoming' | 'past'>('upcoming');
  readonly trips = signal<DriverTrip[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly busy = signal<number | null>(null);

  constructor() {
    this.load();
  }

  select(scope: 'upcoming' | 'past'): void {
    this.scope.set(scope);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.trips(this.scope()).subscribe({
      next: (trips) => {
        this.trips.set(trips);
        this.loading.set(false);
      },
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }

  name(item: { nameFr: string; nameEn: string; nameAr: string }): string {
    return localName(item, this.lang.current());
  }

  when(iso: string): string {
    return formatTunis(iso, this.lang.current());
  }

  /** Cash can be recorded once the trip has started, until it is recorded. */
  canCollect(t: DriverTrip): boolean {
    const b = t.booking;
    return b.paymentMethod === 'CASH' && b.paymentStatus === 'UNPAID' && (b.status === 'IN_PROGRESS' || b.status === 'COMPLETED');
  }

  money(t: DriverTrip): string {
    return formatMoney(t.booking.amount, t.booking.currency, this.lang.current());
  }

  /** Starts or completes a trip; the trip leaves the "upcoming" list once completed. */
  run(trip: DriverTrip, action: Observable<DriverTrip>): void {
    this.busy.set(trip.booking.id);
    this.error.set(null);
    action.subscribe({
      next: (updated) => {
        this.busy.set(null);
        if (updated.booking.status === 'COMPLETED') {
          this.trips.update((list) => list.filter((t) => t.booking.id !== updated.booking.id));
        } else {
          this.trips.update((list) => list.map((t) => (t.booking.id === updated.booking.id ? updated : t)));
        }
      },
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.busy.set(null);
      }
    });
  }
}
