import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { TranslateModule } from '@ngx-translate/core';
import { catchError, combineLatest, forkJoin, map, of, switchMap } from 'rxjs';
import { BookingQuote, BookingService } from '../../core/booking.service';
import { CatalogService, VehicleType } from '../../core/catalog.service';
import { CurrencyService } from '../../core/currency.service';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { formatMoney } from '../../core/money';
import { StickyBarComponent } from '../../shared/sticky-bar.component';
import { BookingDraftService } from './booking-draft.service';
import { RouteSummaryComponent } from './route-summary.component';

type QuoteState = { status: 'loading' } | { status: 'ok'; quote: BookingQuote } | { status: 'unavailable' };

@Component({
  selector: 'app-vehicle-step',
  standalone: true,
  imports: [TranslateModule, StickyBarComponent, RouteSummaryComponent],
  template: `
    <div class="space-y-5 pb-28">
      <app-route-summary />

      <div role="radiogroup" [attr.aria-label]="'booking.steps.vehicle' | translate" class="space-y-3">
        @for (v of vehicleTypes(); track v.id) {
          <button type="button" role="radio"
                  class="block w-full rounded-2xl border-2 bg-white p-4 text-start focus:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  [class.border-brand]="draft.vehicleTypeId() === v.id"
                  [class.bg-brand-soft]="draft.vehicleTypeId() === v.id"
                  [class.border-brand-line]="draft.vehicleTypeId() !== v.id"
                  [attr.aria-checked]="draft.vehicleTypeId() === v.id"
                  [disabled]="!selectable(v)"
                  (click)="draft.vehicleTypeId.set(v.id)">
            <span class="flex items-start justify-between gap-4">
              <span>
                <span class="block text-lg font-extrabold">{{ name(v) }}</span>
                <span class="mt-0.5 block text-sm text-muted">
                  {{ 'booking.vehicle.capacity' | translate: { min: v.minPassengers, max: v.maxPassengers } }}
                </span>
                @if (!fits(v)) {
                  <span class="mt-1 block text-sm font-semibold text-brand-dark">
                    {{ 'booking.vehicle.tooSmall' | translate: { n: passengers() } }}
                  </span>
                }
              </span>
              <span class="text-end">
                @if (quoteOf(v.id); as q) {
                  <span class="block text-lg font-extrabold">{{ money(q.totalAmount, q.leg.currency) }}</span>
                  @if (q.legs > 1) {
                    <span class="block text-xs text-muted">
                      {{ 'booking.perLeg' | translate: { price: money(q.leg.amount, q.leg.currency) } }}
                    </span>
                  }
                } @else if (isUnavailable(v.id)) {
                  <span class="block text-sm font-semibold text-muted">{{ 'booking.vehicle.unavailable' | translate }}</span>
                } @else {
                  <span class="block text-sm text-muted" role="status">{{ 'booking.vehicle.loading' | translate }}</span>
                }
              </span>
            </span>
          </button>
        }
      </div>

      @if (anyDemo()) {
        <p class="rounded-xl bg-white px-4 py-3 text-sm text-muted border border-brand-line" role="note">
          {{ 'booking.demoNotice' | translate }}
        </p>
      }

      <button type="button" class="font-semibold text-brand hover:underline" (click)="draft.go(1)">
        ← {{ 'booking.back' | translate }}
      </button>

      <app-sticky-bar
        [label]="'booking.continue' | translate"
        [totalLabel]="selectedQuote() ? ('booking.total' | translate) : ''"
        [total]="selectedQuote() ? money(selectedQuote()!.totalAmount, selectedQuote()!.leg.currency) : ''"
        [disabled]="!selectedQuote()"
        (action)="draft.go(3)" />
    </div>
  `
})
export class VehicleStepComponent {
  private readonly bookings = inject(BookingService);
  private readonly catalog = inject(CatalogService);
  private readonly currency = inject(CurrencyService);
  private readonly lang = inject(LanguageService);
  readonly draft = inject(BookingDraftService);

  readonly vehicleTypes = toSignal(this.catalog.vehicleTypes(), { initialValue: [] as VehicleType[] });
  private readonly quotes = signal<Record<number, QuoteState>>({});

  readonly passengers = computed(() => this.draft.trip()?.passengers ?? 1);
  readonly selectedQuote = computed(() => {
    const id = this.draft.vehicleTypeId();
    return id === null ? null : this.quoteOf(id);
  });
  readonly anyDemo = computed(() =>
    Object.values(this.quotes()).some((s) => s.status === 'ok' && s.quote.leg.demo)
  );

  constructor() {
    // Prices come from the server and are fetched again whenever the currency changes.
    combineLatest([toObservable(this.currency.current), this.catalog.vehicleTypes()])
      .pipe(
        switchMap(([currency, vehicleTypes]) => {
          this.quotes.set(Object.fromEntries(vehicleTypes.map((v) => [v.id, { status: 'loading' } as QuoteState])));
          return forkJoin(vehicleTypes.map((v) => this.fetch(v, currency))).pipe(
            map((entries) => ({ entries, vehicleTypes }))
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe(({ entries, vehicleTypes }) => {
        this.quotes.set(Object.fromEntries(entries));
        this.ensureSelection(vehicleTypes);
      });
  }

  name(v: VehicleType): string {
    return localName(v, this.lang.current());
  }

  money(amount: number, currency: string): string {
    return formatMoney(amount, currency, this.lang.current());
  }

  fits(v: VehicleType): boolean {
    return this.passengers() <= v.maxPassengers;
  }

  selectable(v: VehicleType): boolean {
    return this.fits(v) && this.quoteOf(v.id) !== null;
  }

  quoteOf(id: number): BookingQuote | null {
    const state = this.quotes()[id];
    return state && state.status === 'ok' ? state.quote : null;
  }

  isUnavailable(id: number): boolean {
    return this.quotes()[id]?.status === 'unavailable';
  }

  private fetch(v: VehicleType, currency: string) {
    const trip = this.draft.trip();
    if (!trip) {
      return of([v.id, { status: 'unavailable' } as QuoteState] as const);
    }
    return this.bookings
      .quote({
        airportId: trip.airportId,
        zoneId: trip.zoneId,
        vehicleTypeId: v.id,
        currency,
        roundTrip: trip.roundTrip
      })
      .pipe(
        map((quote) => [v.id, { status: 'ok', quote } as QuoteState] as const),
        catchError(() => of([v.id, { status: 'unavailable' } as QuoteState] as const))
      );
  }

  /** Keeps a valid selection: the smallest vehicle that fits and has a price. */
  private ensureSelection(vehicleTypes: VehicleType[]): void {
    const current = vehicleTypes.find((v) => v.id === this.draft.vehicleTypeId());
    if (current && this.selectable(current)) {
      return;
    }
    const first = vehicleTypes.find((v) => this.selectable(v));
    this.draft.vehicleTypeId.set(first ? first.id : null);
  }
}
