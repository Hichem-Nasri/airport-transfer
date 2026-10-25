import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Airport, CatalogService, VehicleType, ZoneWithAreas } from '../../core/catalog.service';
import { Direction } from '../../core/booking.service';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { todayInTunis, tunisLocalToIso } from '../../core/tunis-time';
import { StickyBarComponent } from '../../shared/sticky-bar.component';
import { BookingDraftService } from './booking-draft.service';

interface TripFormValue {
  roundTrip: boolean;
  date: string;
  time: string;
  returnDate: string;
  returnTime: string;
}

function isoOf(date: string, time: string): number | null {
  return date && time ? Date.parse(tunisLocalToIso(date, time)) : null;
}

/** Cross-field rules: pickup in the future, and the return after the first trip. */
function tripTimes(group: AbstractControl): ValidationErrors | null {
  const v = group.value as TripFormValue;
  const errors: ValidationErrors = {};
  const out = isoOf(v.date, v.time);
  if (out !== null && out <= Date.now()) {
    errors['pickupPast'] = true;
  }
  if (v.roundTrip) {
    const back = isoOf(v.returnDate, v.returnTime);
    if (out !== null && back !== null && back <= out) {
      errors['returnBeforeOutbound'] = true;
    }
  }
  return Object.keys(errors).length ? errors : null;
}

@Component({
  selector: 'app-trip-step',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, StickyBarComponent],
  template: `
    <form [formGroup]="form" (ngSubmit)="next()" novalidate class="space-y-6 pb-28">
      <div role="group" [attr.aria-label]="'booking.directionLabel' | translate" class="grid grid-cols-2 gap-2 rounded-2xl bg-white p-1.5 border border-brand-line">
        @for (d of directions; track d) {
          <button type="button" class="min-h-11 rounded-xl px-3 text-sm font-semibold"
                  [class.bg-brand]="form.controls.direction.value === d"
                  [class.text-white]="form.controls.direction.value === d"
                  [attr.aria-pressed]="form.controls.direction.value === d"
                  (click)="form.controls.direction.setValue(d)">
            {{ 'booking.direction.' + d | translate }}
          </button>
        }
      </div>

      <label class="flex min-h-11 items-center gap-3 font-semibold">
        <input type="checkbox" class="h-5 w-5 accent-[#D2001A]" formControlName="roundTrip" />
        {{ 'booking.roundTrip' | translate }}
      </label>

      <div>
        <label class="field-label" for="airportId">{{ 'booking.fields.airport' | translate }}</label>
        <select id="airportId" class="field-input" formControlName="airportId"
                [attr.aria-invalid]="form.controls.airportId.touched && form.controls.airportId.invalid">
          <option [ngValue]="null" disabled>{{ 'booking.choose' | translate }}</option>
          @for (a of airports(); track a.id) {
            <option [ngValue]="a.id">{{ a.iataCode }} — {{ name(a) }}</option>
          }
        </select>
        @if (form.controls.airportId.touched && form.controls.airportId.invalid) {
          <p class="field-error">{{ 'validation.required' | translate }}</p>
        }
      </div>

      <div>
        <label class="field-label" for="areaId">{{ 'booking.fields.destination' | translate }}</label>
        <select id="areaId" class="field-input" formControlName="areaId"
                [attr.aria-invalid]="form.controls.areaId.touched && form.controls.areaId.invalid">
          <option [ngValue]="null" disabled>{{ 'booking.choose' | translate }}</option>
          @for (a of areas(); track a.id) {
            <option [ngValue]="a.id">{{ a.label }}</option>
          }
        </select>
        @if (form.controls.areaId.touched && form.controls.areaId.invalid) {
          <p class="field-error">{{ 'validation.required' | translate }}</p>
        }
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="field-label" for="date">{{ 'booking.fields.date' | translate }}</label>
          <input id="date" type="date" class="field-input" formControlName="date" [min]="today"
                 [attr.aria-invalid]="form.controls.date.touched && form.controls.date.invalid" />
        </div>
        <div>
          <label class="field-label" for="time">{{ 'booking.fields.time' | translate }}</label>
          <input id="time" type="time" class="field-input" formControlName="time"
                 [attr.aria-invalid]="form.controls.time.touched && form.controls.time.invalid" />
        </div>
      </div>
      @if (showRequired('date') || showRequired('time')) {
        <p class="field-error">{{ 'validation.required' | translate }}</p>
      }
      @if (form.touched && form.hasError('pickupPast')) {
        <p class="field-error" role="alert">{{ 'validation.future' | translate }}</p>
      }

      @if (form.controls.roundTrip.value) {
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="field-label" for="returnDate">{{ 'booking.fields.returnDate' | translate }}</label>
            <input id="returnDate" type="date" class="field-input" formControlName="returnDate"
                   [min]="form.controls.date.value || today"
                   [attr.aria-invalid]="form.controls.returnDate.touched && form.controls.returnDate.invalid" />
          </div>
          <div>
            <label class="field-label" for="returnTime">{{ 'booking.fields.returnTime' | translate }}</label>
            <input id="returnTime" type="time" class="field-input" formControlName="returnTime"
                   [attr.aria-invalid]="form.controls.returnTime.touched && form.controls.returnTime.invalid" />
          </div>
        </div>
        @if (showRequired('returnDate') || showRequired('returnTime')) {
          <p class="field-error">{{ 'validation.required' | translate }}</p>
        }
        @if (form.touched && form.hasError('returnBeforeOutbound')) {
          <p class="field-error" role="alert">{{ 'validation.returnAfter' | translate }}</p>
        }
      }

      <div>
        <label class="field-label" for="passengers">{{ 'booking.fields.passengers' | translate }}</label>
        <div class="mt-1 flex items-center gap-3">
          <button type="button" class="btn-secondary w-11 px-0 text-xl" (click)="step(-1)"
                  [attr.aria-label]="'booking.fewer' | translate" [disabled]="form.controls.passengers.value <= 1">−</button>
          <input id="passengers" type="number" inputmode="numeric" min="1" [max]="maxPassengers()"
                 class="field-input mt-0 w-20 text-center" formControlName="passengers" />
          <button type="button" class="btn-secondary w-11 px-0 text-xl" (click)="step(1)"
                  [attr.aria-label]="'booking.more' | translate"
                  [disabled]="form.controls.passengers.value >= maxPassengers()">+</button>
        </div>
        @if (form.controls.passengers.invalid) {
          <p class="field-error">{{ 'validation.range' | translate: { min: 1, max: maxPassengers() } }}</p>
        }
      </div>

      <app-sticky-bar [label]="'booking.continue' | translate" (action)="next()" />
    </form>
  `
})
export class TripStepComponent {
  private readonly catalog = inject(CatalogService);
  private readonly lang = inject(LanguageService);
  private readonly draft = inject(BookingDraftService);

  readonly directions: Direction[] = ['AIRPORT_TO_ADDRESS', 'ADDRESS_TO_AIRPORT'];
  readonly today = todayInTunis();

  readonly airports = toSignal(this.catalog.airports(), { initialValue: [] as Airport[] });
  private readonly zones = toSignal(this.catalog.zones(), { initialValue: [] as ZoneWithAreas[] });
  private readonly vehicleTypes = toSignal(this.catalog.vehicleTypes(), { initialValue: [] as VehicleType[] });

  /** The customer picks a governorate; its pricing zone is derived from it. */
  readonly areas = computed(() =>
    this.zones()
      .flatMap((z) => z.areas.map((a) => ({ id: a.id, zoneId: z.id, label: localName(a, this.lang.current()) })))
      .sort((a, b) => a.label.localeCompare(b.label, this.lang.current()))
  );

  /** A bigger vehicle can always carry fewer people: the limit is the largest vehicle. */
  readonly maxPassengers = computed(() => Math.max(1, ...this.vehicleTypes().map((v) => v.maxPassengers)));

  private readonly saved = this.draft.trip();

  readonly form = inject(FormBuilder).nonNullable.group(
    {
      direction: [this.saved?.direction ?? ('AIRPORT_TO_ADDRESS' as Direction)],
      roundTrip: [this.saved?.roundTrip ?? false],
      airportId: [this.saved?.airportId ?? (null as number | null), Validators.required],
      areaId: [this.saved?.areaId ?? (null as number | null), Validators.required],
      date: [this.saved?.date ?? '', Validators.required],
      time: [this.saved?.time ?? '', Validators.required],
      returnDate: [this.saved?.returnDate ?? ''],
      returnTime: [this.saved?.returnTime ?? ''],
      passengers: [
        this.saved?.passengers ?? 1,
        [
          Validators.required,
          Validators.min(1),
          (c: AbstractControl): ValidationErrors | null => (c.value > this.maxPassengers() ? { max: true } : null)
        ]
      ]
    },
    { validators: tripTimes }
  );

  constructor() {
    // The return date and time are required only for a round trip.
    this.form.controls.roundTrip.valueChanges.subscribe((roundTrip) => {
      for (const c of [this.form.controls.returnDate, this.form.controls.returnTime]) {
        c.setValidators(roundTrip ? Validators.required : null);
        c.updateValueAndValidity();
      }
    });
    if (this.saved?.roundTrip) {
      this.form.controls.roundTrip.setValue(true);
    }
  }

  name(item: Airport): string {
    return localName(item, this.lang.current());
  }

  showRequired(control: 'date' | 'time' | 'returnDate' | 'returnTime'): boolean {
    const c = this.form.controls[control];
    return c.touched && c.hasError('required');
  }

  step(delta: number): void {
    const c = this.form.controls.passengers;
    c.setValue(Math.min(this.maxPassengers(), Math.max(1, c.value + delta)));
  }

  next(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const v = this.form.getRawValue();
    const area = this.areas().find((a) => a.id === v.areaId);
    if (v.airportId === null || v.areaId === null || !area) {
      return;
    }
    this.draft.trip.set({
      direction: v.direction,
      roundTrip: v.roundTrip,
      airportId: v.airportId,
      areaId: v.areaId,
      zoneId: area.zoneId,
      date: v.date,
      time: v.time,
      returnDate: v.returnDate,
      returnTime: v.returnTime,
      passengers: v.passengers,
      pickupAt: tunisLocalToIso(v.date, v.time),
      returnPickupAt: v.roundTrip ? tunisLocalToIso(v.returnDate, v.returnTime) : null
    });
    this.draft.go(2);
  }
}
