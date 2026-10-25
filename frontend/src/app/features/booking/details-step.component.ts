import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { catchError, of, switchMap } from 'rxjs';
import { errorKey } from '../../core/api-error';
import { BookingQuote, BookingService, CreateBookingRequest } from '../../core/booking.service';
import { CatalogService, VehicleType } from '../../core/catalog.service';
import { CurrencyService } from '../../core/currency.service';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { formatMoney } from '../../core/money';
import { StickyBarComponent } from '../../shared/sticky-bar.component';
import { BookingDraftService } from './booking-draft.service';
import { RouteSummaryComponent } from './route-summary.component';

const FLIGHT_PATTERN = /^$|^[A-Za-z0-9 ]{2,10}$/;

@Component({
  selector: 'app-details-step',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, StickyBarComponent, RouteSummaryComponent],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate class="space-y-6 pb-28">
      <app-route-summary />

      @if (vehicle(); as v) {
        <p class="rounded-2xl border border-brand-line bg-white p-4 font-semibold">
          {{ name(v) }} · {{ 'booking.vehicle.capacity' | translate: { min: v.minPassengers, max: v.maxPassengers } }}
        </p>
      }

      <div>
        <label class="field-label" for="address">{{ 'booking.fields.address' | translate }}</label>
        <textarea id="address" rows="2" class="field-input py-2" formControlName="address" autocomplete="street-address"
                  [attr.aria-invalid]="form.controls.address.touched && form.controls.address.invalid"
                  aria-describedby="address-hint"></textarea>
        <p id="address-hint" class="mt-1 text-sm text-muted">{{ 'booking.hints.address' | translate }}</p>
        @if (form.controls.address.touched && form.controls.address.invalid) {
          <p class="field-error">{{ 'validation.required' | translate }}</p>
        }
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="field-label" for="flightNumber">{{ 'booking.fields.flightNumber' | translate }}</label>
          <input id="flightNumber" type="text" class="field-input uppercase" formControlName="flightNumber" maxlength="10"
                 placeholder="TU 714" [attr.aria-invalid]="form.controls.flightNumber.invalid" />
          @if (form.controls.flightNumber.invalid) {
            <p class="field-error">{{ 'validation.flight' | translate }}</p>
          }
        </div>
        <div>
          <label class="field-label" for="luggageCount">{{ 'booking.fields.luggage' | translate }}</label>
          <input id="luggageCount" type="number" inputmode="numeric" min="0" max="100" class="field-input"
                 formControlName="luggageCount" [attr.aria-invalid]="form.controls.luggageCount.invalid" />
          @if (form.controls.luggageCount.invalid) {
            <p class="field-error">{{ 'validation.range' | translate: { min: 0, max: 100 } }}</p>
          }
        </div>
      </div>

      @if (trip()?.roundTrip) {
        <div>
          <label class="field-label" for="returnFlightNumber">{{ 'booking.fields.returnFlightNumber' | translate }}</label>
          <input id="returnFlightNumber" type="text" class="field-input uppercase" formControlName="returnFlightNumber"
                 maxlength="10" placeholder="TU 715" [attr.aria-invalid]="form.controls.returnFlightNumber.invalid" />
          @if (form.controls.returnFlightNumber.invalid) {
            <p class="field-error">{{ 'validation.flight' | translate }}</p>
          }
        </div>
      }

      <div>
        <label class="field-label" for="notes">{{ 'booking.fields.notes' | translate }}</label>
        <textarea id="notes" rows="3" class="field-input py-2" formControlName="notes" maxlength="1000"></textarea>
      </div>

      <fieldset>
        <legend class="field-label">{{ 'booking.fields.paymentMethod' | translate }}</legend>
        <div class="mt-2 space-y-2">
          <label class="flex min-h-11 items-center gap-3 rounded-xl border-2 border-brand bg-brand-soft px-4 font-semibold">
            <input type="radio" class="h-5 w-5 accent-[#D2001A]" checked name="payment" value="CASH" />
            {{ 'booking.payment.CASH' | translate }}
          </label>
          <label class="flex min-h-11 items-center gap-3 rounded-xl border border-brand-line bg-white px-4 text-muted opacity-60">
            <input type="radio" class="h-5 w-5" disabled name="payment" value="CARD" />
            {{ 'booking.payment.CARD' | translate }} — {{ 'booking.payment.soon' | translate }}
          </label>
        </div>
      </fieldset>

      @if (error(); as key) {
        <p class="rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">
          {{ key | translate }}
        </p>
      }

      <button type="button" class="font-semibold text-brand hover:underline" (click)="draft.go(2)">
        ← {{ 'booking.back' | translate }}
      </button>

      <app-sticky-bar
        [label]="'booking.confirm' | translate"
        [totalLabel]="quote() ? ('booking.total' | translate) : ''"
        [total]="quote() ? money(quote()!) : ''"
        [disabled]="loading()"
        (action)="submit()" />
    </form>
  `
})
export class DetailsStepComponent {
  private readonly bookings = inject(BookingService);
  private readonly catalog = inject(CatalogService);
  private readonly currency = inject(CurrencyService);
  private readonly lang = inject(LanguageService);
  readonly draft = inject(BookingDraftService);

  private readonly vehicleTypes = toSignal(this.catalog.vehicleTypes(), { initialValue: [] as VehicleType[] });
  readonly trip = this.draft.trip;
  readonly vehicle = computed(() => this.vehicleTypes().find((v) => v.id === this.draft.vehicleTypeId()) ?? null);
  readonly quote = signal<BookingQuote | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = inject(FormBuilder).nonNullable.group({
    address: ['', [Validators.required, Validators.maxLength(500)]],
    flightNumber: ['', Validators.pattern(FLIGHT_PATTERN)],
    returnFlightNumber: ['', Validators.pattern(FLIGHT_PATTERN)],
    luggageCount: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    notes: ['', Validators.maxLength(1000)]
  });

  constructor() {
    // The total shown in the bar is the server's preview, fetched again if the currency changes.
    toObservable(this.currency.current)
      .pipe(
        switchMap((currency) => {
          const trip = this.draft.trip();
          const vehicleTypeId = this.draft.vehicleTypeId();
          if (!trip || vehicleTypeId === null) {
            return of(null);
          }
          return this.bookings
            .quote({
              airportId: trip.airportId,
              zoneId: trip.zoneId,
              vehicleTypeId,
              currency,
              roundTrip: trip.roundTrip
            })
            .pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed()
      )
      .subscribe((q) => this.quote.set(q));
  }

  name(v: VehicleType): string {
    return localName(v, this.lang.current());
  }

  money(q: BookingQuote): string {
    return formatMoney(q.totalAmount, q.leg.currency, this.lang.current());
  }

  submit(): void {
    const trip = this.draft.trip();
    const vehicleTypeId = this.draft.vehicleTypeId();
    this.form.markAllAsTouched();
    if (this.form.invalid || !trip || vehicleTypeId === null || this.loading()) {
      return;
    }
    const v = this.form.getRawValue();
    const request: CreateBookingRequest = {
      airportId: trip.airportId,
      direction: trip.direction,
      zoneId: trip.zoneId,
      address: v.address.trim(),
      pickupAt: trip.pickupAt,
      passengers: trip.passengers,
      luggageCount: v.luggageCount,
      vehicleTypeId,
      currency: this.currency.current(),
      paymentMethod: 'CASH',
      ...(v.flightNumber.trim() ? { flightNumber: v.flightNumber.trim() } : {}),
      ...(v.notes.trim() ? { notes: v.notes.trim() } : {}),
      ...(trip.returnPickupAt ? { returnPickupAt: trip.returnPickupAt } : {}),
      ...(trip.returnPickupAt && v.returnFlightNumber.trim()
        ? { returnFlightNumber: v.returnFlightNumber.trim() }
        : {})
    };
    this.loading.set(true);
    this.error.set(null);
    this.bookings.create(request).subscribe({
      next: (created) => this.draft.created.set(created),
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }
}
