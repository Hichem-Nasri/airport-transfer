import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { errorKey } from '../../core/api-error';
import { Booking, BookingService, BookingStatus } from '../../core/booking.service';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { formatMoney } from '../../core/money';
import { formatTunis } from '../../core/tunis-time';

const BADGES: Record<BookingStatus, string> = {
  PENDING: 'bg-brand-soft text-brand-dark',
  CONFIRMED: 'bg-ink text-white',
  ASSIGNED: 'bg-ink text-white',
  IN_PROGRESS: 'bg-ink text-white',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-gray-200 text-muted'
};

@Component({
  selector: 'app-bookings',
  standalone: true,
  imports: [RouterLink, TranslateModule],
  template: `
    <section class="mx-auto w-full max-w-2xl px-4 py-8 sm:px-6">
      <div class="flex items-center justify-between gap-4">
        <h1 class="text-3xl font-extrabold">{{ 'bookings.title' | translate }}</h1>
        <a routerLink="/book" class="btn-primary">{{ 'nav.book' | translate }}</a>
      </div>

      @if (error(); as key) {
        <p class="mt-6 rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
      }

      @if (loading()) {
        <p class="mt-8 text-muted" role="status">{{ 'account.loading' | translate }}</p>
      } @else if (bookings().length === 0 && !error()) {
        <div class="mt-8 rounded-2xl border border-brand-line bg-white p-6 text-center">
          <p class="font-semibold">{{ 'bookings.empty' | translate }}</p>
          <a routerLink="/book" class="btn-primary mt-4">{{ 'bookings.emptyCta' | translate }}</a>
        </div>
      } @else {
        <ul class="mt-6 space-y-4">
          @for (b of bookings(); track b.id) {
            <li class="rounded-2xl border border-brand-line bg-white p-4">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <p class="text-lg font-extrabold tracking-wide" dir="ltr">{{ b.reference }}</p>
                <span class="rounded-full px-3 py-1 text-sm font-semibold" [class]="badge(b.status)">
                  {{ 'status.' + b.status | translate }}
                </span>
              </div>
              @if (b.groupId) {
                <p class="mt-1 text-sm font-semibold text-brand">{{ 'bookings.roundTrip' | translate }}</p>
              }
              <p class="mt-3 flex flex-wrap items-center gap-x-2 font-semibold">
                <span>{{ from(b) }}</span>
                <span aria-hidden="true" class="text-brand rtl:rotate-180">→</span>
                <span>{{ to(b) }}</span>
              </p>
              <p class="mt-1 text-sm text-muted">{{ when(b.pickupAt) }}</p>
              <dl class="mt-3 grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
                <dt class="text-muted">{{ 'booking.fields.address' | translate }}</dt>
                <dd class="font-semibold">{{ b.address }}</dd>
                <dt class="text-muted">{{ 'booking.fields.passengers' | translate }}</dt>
                <dd class="font-semibold">{{ b.passengers }} · {{ vehicle(b) }}</dd>
                @if (b.flightNumber) {
                  <dt class="text-muted">{{ 'booking.fields.flightNumber' | translate }}</dt>
                  <dd class="font-semibold" dir="ltr">{{ b.flightNumber }}</dd>
                }
                <dt class="text-muted">{{ 'booking.fields.paymentMethod' | translate }}</dt>
                <dd class="font-semibold">{{ 'booking.payment.' + b.paymentMethod | translate }}</dd>
              </dl>
              <p class="mt-3 text-lg font-extrabold">{{ money(b) }}</p>

              @if (b.cancellable) {
                @if (confirming() === b.id) {
                  <div class="mt-3 rounded-xl bg-brand-soft p-3" role="alertdialog" [attr.aria-label]="'bookings.cancelConfirm' | translate">
                    <p class="text-sm font-semibold">{{ 'bookings.cancelConfirm' | translate }}</p>
                    <div class="mt-2 flex gap-2">
                      <button type="button" class="btn-primary" [disabled]="busy() === b.id" (click)="cancel(b)">
                        {{ 'bookings.cancelYes' | translate }}
                      </button>
                      <button type="button" class="btn-secondary" (click)="confirming.set(null)">
                        {{ 'bookings.cancelNo' | translate }}
                      </button>
                    </div>
                  </div>
                } @else {
                  <button type="button" class="btn-secondary mt-3" (click)="confirming.set(b.id)">
                    {{ 'bookings.cancel' | translate }}
                  </button>
                }
              }
            </li>
          }
        </ul>
      }
    </section>
  `
})
export class BookingsComponent {
  private readonly service = inject(BookingService);
  private readonly lang = inject(LanguageService);

  readonly bookings = signal<Booking[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly confirming = signal<number | null>(null);
  readonly busy = signal<number | null>(null);

  constructor() {
    this.service.mine().subscribe({
      next: (list) => {
        this.bookings.set(list);
        this.loading.set(false);
      },
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }

  badge(status: BookingStatus): string {
    return BADGES[status];
  }

  from(b: Booking): string {
    return b.direction === 'AIRPORT_TO_ADDRESS' ? this.airport(b) : localName(b.zone, this.lang.current());
  }

  to(b: Booking): string {
    return b.direction === 'AIRPORT_TO_ADDRESS' ? localName(b.zone, this.lang.current()) : this.airport(b);
  }

  vehicle(b: Booking): string {
    return localName(b.vehicleType, this.lang.current());
  }

  when(iso: string): string {
    return formatTunis(iso, this.lang.current());
  }

  /** The amount and currency stored in the booking when it was made. */
  money(b: Booking): string {
    return formatMoney(b.amount, b.currency, this.lang.current());
  }

  cancel(b: Booking): void {
    this.busy.set(b.id);
    this.error.set(null);
    this.service.cancel(b.id).subscribe({
      next: (updated) => {
        this.bookings.update((list) => list.map((x) => (x.id === updated.id ? updated : x)));
        this.confirming.set(null);
        this.busy.set(null);
      },
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.busy.set(null);
      }
    });
  }

  private airport(b: Booking): string {
    return `${b.airport.iataCode} · ${localName(b.airport, this.lang.current())}`;
  }
}
