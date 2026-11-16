import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Airport, CatalogService, VehicleType, ZoneWithAreas } from '../../core/catalog.service';
import { CurrencyService } from '../../core/currency.service';
import { PackQuote, PackService } from '../../core/pack.service';
import { Direction } from '../../core/booking.service';
import { errorKey } from '../../core/api-error';
import { LanguageService } from '../../core/language.service';
import { Localized, localName } from '../../core/localized';
import { localToIso, todayIn } from '../../core/zoned-time';
import { formatMoney } from '../../core/money';

const DRAFT_KEY = 'hms.pack.draft';

type LegGroup = FormGroup<{
  airportId: FormControl<number | null>;
  direction: FormControl<Direction>;
  areaId: FormControl<number | null>;
  address: FormControl<string>;
  flightNumber: FormControl<string>;
  date: FormControl<string>;
  time: FormControl<string>;
  vehicleTypeId: FormControl<number | null>;
  passengers: FormControl<number>;
  luggageCount: FormControl<number>;
}>;

/** A pack books linked trips in one go (for instance Paris to Orly, then Tunis-Carthage to the hotel). */
@Component({
  selector: 'app-pack',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, RouterLink],
  template: `
    <section class="mx-auto max-w-2xl px-4 py-6 pb-32">
      <h1 class="text-2xl font-extrabold">{{ 'pack.title' | translate }}</h1>
      <p class="mt-1 text-muted">{{ 'pack.subtitle' | translate }}</p>

      @if (done(); as created) {
        <div class="mt-6 rounded-2xl border border-brand-line bg-brand-soft p-5" role="status">
          <h2 class="font-extrabold">{{ 'pack.done' | translate }}</h2>
          <ul class="mt-2 list-disc ps-5">
            @for (b of created; track b) {
              <li dir="ltr">{{ b }}</li>
            }
          </ul>
          <a routerLink="/bookings" class="btn-primary mt-4">{{ 'bookings.title' | translate }}</a>
        </div>
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate class="mt-6 space-y-6">
          <div formArrayName="legs" class="space-y-6">
            @for (leg of legs.controls; track leg; let i = $index) {
              <fieldset [formGroupName]="i" class="space-y-4 rounded-2xl border border-brand-line bg-white p-4">
                <legend class="px-2 font-extrabold">{{ 'pack.leg' | translate: { n: i + 1 } }}</legend>

                <div role="group" [attr.aria-label]="'booking.directionLabel' | translate" class="grid grid-cols-2 gap-2">
                  @for (d of directions; track d) {
                    <button type="button" class="min-h-11 rounded-xl border border-brand-line px-3 text-sm font-semibold"
                            [class.bg-brand]="leg.controls.direction.value === d"
                            [class.text-white]="leg.controls.direction.value === d"
                            [attr.aria-pressed]="leg.controls.direction.value === d"
                            (click)="leg.controls.direction.setValue(d)">
                      {{ 'booking.direction.' + d | translate }}
                    </button>
                  }
                </div>

                <div>
                  <label class="field-label" [for]="'airport-' + i">{{ 'booking.fields.airport' | translate }}</label>
                  <select [id]="'airport-' + i" class="field-input" formControlName="airportId" (change)="airportChanged(leg)">
                    <option [ngValue]="null" disabled>{{ 'booking.choose' | translate }}</option>
                    @for (a of airports(); track a.id) {
                      <option [ngValue]="a.id">{{ a.iataCode }} — {{ name(a) }}</option>
                    }
                  </select>
                </div>

                <div>
                  <label class="field-label" [for]="'area-' + i">{{ 'booking.fields.destination' | translate }}</label>
                  <select [id]="'area-' + i" class="field-input" formControlName="areaId">
                    <option [ngValue]="null" disabled>{{ 'booking.choose' | translate }}</option>
                    @for (a of areasFor(leg); track a.id) {
                      <option [ngValue]="a.id">{{ a.label }}</option>
                    }
                  </select>
                </div>

                <div>
                  <label class="field-label" [for]="'address-' + i">{{ 'booking.fields.address' | translate }}</label>
                  <input [id]="'address-' + i" class="field-input" formControlName="address" maxlength="500" />
                </div>

                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="field-label" [for]="'date-' + i">{{ 'booking.fields.date' | translate }}</label>
                    <input [id]="'date-' + i" type="date" class="field-input" formControlName="date" [min]="today" />
                  </div>
                  <div>
                    <label class="field-label" [for]="'time-' + i">{{ 'booking.fields.time' | translate }}</label>
                    <input [id]="'time-' + i" type="time" class="field-input" formControlName="time" />
                  </div>
                </div>

                <div>
                  <label class="field-label" [for]="'vehicle-' + i">{{ 'pack.vehicle' | translate }}</label>
                  <select [id]="'vehicle-' + i" class="field-input" formControlName="vehicleTypeId">
                    <option [ngValue]="null" disabled>{{ 'booking.choose' | translate }}</option>
                    @for (v of vehicleTypes(); track v.id) {
                      <option [ngValue]="v.id">{{ name(v) }}{{ v.models ? ' — ' + v.models : '' }}</option>
                    }
                  </select>
                </div>

                <div class="grid grid-cols-3 gap-3">
                  <div>
                    <label class="field-label" [for]="'pax-' + i">{{ 'booking.fields.passengers' | translate }}</label>
                    <input [id]="'pax-' + i" type="number" min="1" max="100" class="field-input" formControlName="passengers" />
                  </div>
                  <div>
                    <label class="field-label" [for]="'bags-' + i">{{ 'booking.fields.luggage' | translate }}</label>
                    <input [id]="'bags-' + i" type="number" min="0" max="100" class="field-input" formControlName="luggageCount" />
                  </div>
                  <div>
                    <label class="field-label" [for]="'flight-' + i">{{ 'booking.fields.flightNumber' | translate }}</label>
                    <input [id]="'flight-' + i" class="field-input uppercase" formControlName="flightNumber" maxlength="10" />
                  </div>
                </div>

                @if (legs.length > 2) {
                  <button type="button" class="btn-secondary" (click)="removeLeg(i)">{{ 'pack.removeLeg' | translate }}</button>
                }
              </fieldset>
            }
          </div>

          @if (legs.length < 4) {
            <button type="button" class="btn-secondary w-full" (click)="addLeg()">{{ 'pack.addLeg' | translate }}</button>
          }

          @if (error(); as key) {
            <p class="rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
          }

          <div class="fixed inset-x-0 bottom-0 border-t border-brand-line bg-white px-4 py-3">
            <div class="mx-auto flex max-w-2xl items-center justify-between gap-3">
              <div>
                <div class="text-xs text-muted">{{ 'pack.total' | translate }}</div>
                @if (quote(); as q) {
                  <div class="text-xl font-extrabold" dir="ltr">{{ money(q) }}</div>
                } @else {
                  <div class="text-xl font-extrabold">—</div>
                }
              </div>
              <button type="submit" class="btn-primary" [disabled]="saving()">{{ 'pack.confirm' | translate }}</button>
            </div>
          </div>
        </form>
      }
    </section>
  `
})
export class PackComponent {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly catalog = inject(CatalogService);
  private readonly packs = inject(PackService);
  private readonly lang = inject(LanguageService);
  private readonly currency = inject(CurrencyService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly directions: Direction[] = ['AIRPORT_TO_ADDRESS', 'ADDRESS_TO_AIRPORT'];
  readonly today = todayIn('Europe/Paris');
  readonly airports = toSignal(this.catalog.airports(), { initialValue: [] as Airport[] });
  private readonly zones = toSignal(this.catalog.zones(), { initialValue: [] as ZoneWithAreas[] });
  readonly vehicleTypes = toSignal(this.catalog.vehicleTypes(), { initialValue: [] as VehicleType[] });

  readonly quote = signal<PackQuote | null>(null);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly done = signal<string[] | null>(null);
  /** Bumped on every form change so the quote effect re-runs. */
  private readonly tick = signal(0);

  readonly form = this.fb.group({
    legs: this.fb.array<LegGroup>([this.newLeg('ADDRESS_TO_AIRPORT'), this.newLeg('AIRPORT_TO_ADDRESS')])
  });

  get legs(): FormArray<LegGroup> {
    return this.form.controls.legs;
  }

  private readonly selection = computed(() => {
    this.tick();
    this.currency.current();
    return this.legs.controls.map((l) => {
      const v = l.getRawValue();
      return { airportId: v.airportId, zoneId: this.zoneIdOf(v.areaId), vehicleTypeId: v.vehicleTypeId };
    });
  });

  constructor() {
    this.restoreDraft();
    this.form.valueChanges.subscribe(() => this.tick.update((n) => n + 1));
    effect(
      () => {
        const sel = this.selection();
        const ready = sel.every((s) => s.airportId !== null && s.zoneId !== null && s.vehicleTypeId !== null);
        if (!ready) {
          this.quote.set(null);
          return;
        }
        this.packs
          .quote(
            this.currency.current(),
            sel.map((s) => ({ airportId: s.airportId!, zoneId: s.zoneId!, vehicleTypeId: s.vehicleTypeId! }))
          )
          .subscribe({
            next: (q) => {
              this.error.set(null);
              this.quote.set(q);
            },
            error: (e: unknown) => {
              this.quote.set(null);
              this.error.set(errorKey(e));
            }
          });
      },
      { allowSignalWrites: true }
    );
  }

  money(q: PackQuote): string {
    return formatMoney(q.totalAmount, q.currency, this.lang.current());
  }

  name(item: Localized): string {
    return localName(item, this.lang.current());
  }

  areasFor(leg: LegGroup): { id: number; zoneId: number; label: string }[] {
    const country = this.airports().find((a) => a.id === leg.controls.airportId.value)?.country;
    return this.zones()
      .filter((z) => !country || z.country === country)
      .flatMap((z) => z.areas.map((a) => ({ id: a.id, zoneId: z.id, label: localName(a, this.lang.current()) })))
      .sort((a, b) => a.label.localeCompare(b.label, this.lang.current()));
  }

  airportChanged(leg: LegGroup): void {
    leg.controls.areaId.setValue(null);
  }

  addLeg(): void {
    const last = this.legs.at(this.legs.length - 1).controls.direction.value;
    this.legs.push(this.newLeg(last === 'AIRPORT_TO_ADDRESS' ? 'ADDRESS_TO_AIRPORT' : 'AIRPORT_TO_ADDRESS'));
  }

  removeLeg(index: number): void {
    this.legs.removeAt(index);
  }

  submit(): void {
    this.error.set(null);
    if (!this.auth.isLoggedIn()) {
      this.saveDraft();
      void this.router.navigate(['/login'], { queryParams: { returnUrl: '/pack' } });
      return;
    }
    const requests = [];
    for (const leg of this.legs.controls) {
      const v = leg.getRawValue();
      const airport = this.airports().find((a) => a.id === v.airportId);
      const zoneId = this.zoneIdOf(v.areaId);
      if (!airport || zoneId === null || v.vehicleTypeId === null || !v.address.trim() || !v.date || !v.time) {
        leg.markAllAsTouched();
        this.error.set('pack.incomplete');
        return;
      }
      requests.push({
        airportId: airport.id,
        direction: v.direction,
        zoneId,
        address: v.address.trim(),
        flightNumber: v.flightNumber.trim() || undefined,
        pickupAt: localToIso(v.date, v.time, airport.timeZone),
        passengers: v.passengers,
        luggageCount: v.luggageCount,
        vehicleTypeId: v.vehicleTypeId
      });
    }
    this.saving.set(true);
    this.packs.create({ legs: requests, currency: this.currency.current(), paymentMethod: 'CASH' }).subscribe({
      next: (r) => {
        this.saving.set(false);
        this.done.set(r.bookings.map((b) => b.reference));
      },
      error: (e: unknown) => {
        this.saving.set(false);
        this.error.set(errorKey(e));
      }
    });
  }

  /** An anonymous visitor fills the pack, then signs in: the form is kept for the round trip to the login page. */
  private saveDraft(): void {
    try {
      sessionStorage.setItem(DRAFT_KEY, JSON.stringify(this.legs.getRawValue()));
    } catch {
      // storage unavailable: the visitor simply fills the form again
    }
  }

  private restoreDraft(): void {
    try {
      const raw = sessionStorage.getItem(DRAFT_KEY);
      if (!raw) {
        return;
      }
      sessionStorage.removeItem(DRAFT_KEY);
      const saved = JSON.parse(raw) as ReturnType<LegGroup['getRawValue']>[];
      if (!Array.isArray(saved) || saved.length < 2 || saved.length > 4) {
        return;
      }
      while (this.legs.length < saved.length) {
        this.addLeg();
      }
      this.legs.patchValue(saved);
    } catch {
      // unreadable draft: ignore it
    }
  }

  private zoneIdOf(areaId: number | null): number | null {
    if (areaId === null) {
      return null;
    }
    return this.zones().find((z) => z.areas.some((a) => a.id === areaId))?.id ?? null;
  }

  private newLeg(direction: Direction): LegGroup {
    return this.fb.group({
      airportId: this.fb.control<number | null>(null, Validators.required),
      direction: this.fb.control<Direction>(direction),
      areaId: this.fb.control<number | null>(null, Validators.required),
      address: this.fb.control('', Validators.required),
      flightNumber: this.fb.control(''),
      date: this.fb.control('', Validators.required),
      time: this.fb.control('', Validators.required),
      vehicleTypeId: this.fb.control<number | null>(null, Validators.required),
      passengers: this.fb.control(1, [Validators.required, Validators.min(1)]),
      luggageCount: this.fb.control(0, [Validators.min(0)])
    });
  }
}
