import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Observable, catchError, map, of, startWith, switchMap } from 'rxjs';
import { BookingService, Direction } from '../../core/booking.service';
import { Airport, CatalogService, VehicleType, ZoneWithAreas } from '../../core/catalog.service';
import { CurrencyService } from '../../core/currency.service';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { formatMoney } from '../../core/money';
import { todayInTunis } from '../../core/tunis-time';

type Preview =
  | { state: 'idle' | 'loading' | 'none' }
  | { state: 'ok'; amount: number; currency: string; vehicle: VehicleType; demo: boolean };

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [FormsModule, RouterLink, TranslateModule],
  template: `
    <section class="bg-brand-soft/60 px-4 pb-20 pt-10 sm:px-6 md:px-12 md:pb-28 md:pt-16">
      <div class="mx-auto grid w-full max-w-6xl items-start gap-10 lg:grid-cols-2">
        <div>
          <span class="inline-block rounded-full bg-[#FDE3E5] px-3.5 py-1.5 text-sm font-semibold text-brand-dark">
            {{ 'home.badge' | translate }}
          </span>
          <h1 class="mt-5 text-4xl font-extrabold leading-tight md:text-6xl">{{ 'home.title' | translate }}</h1>
          <p class="mt-4 max-w-lg text-lg leading-relaxed text-muted">{{ 'home.subtitle' | translate }}</p>
          <ul class="mt-6 flex flex-wrap gap-x-6 gap-y-2 text-sm font-semibold">
            <li>✓ {{ 'home.points.fixed' | translate }}</li>
            <li>✓ {{ 'home.points.meet' | translate }}</li>
            <li>✓ {{ 'home.points.currency' | translate }}</li>
          </ul>
        </div>

        <form (ngSubmit)="go()" class="rounded-3xl border border-brand-line bg-white p-5 shadow-xl shadow-brand/10 sm:p-6"
              [attr.aria-label]="'home.card.title' | translate">
          <h2 class="text-xl font-extrabold">{{ 'home.card.title' | translate }}</h2>

          <div role="group" [attr.aria-label]="'booking.directionLabel' | translate"
               class="mt-4 grid grid-cols-2 gap-2 rounded-2xl border border-brand-line bg-brand-soft p-1.5">
            @for (d of directions; track d) {
              <button type="button" class="min-h-11 rounded-xl px-3 text-sm font-semibold"
                      [class.bg-brand]="direction() === d" [class.text-white]="direction() === d"
                      [attr.aria-pressed]="direction() === d" (click)="direction.set(d)">
                {{ 'booking.direction.' + d | translate }}
              </button>
            }
          </div>

          <div class="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <label class="field-label" for="h-airport">{{ 'booking.fields.airport' | translate }}</label>
              <select id="h-airport" name="airportId" class="field-input" [ngModel]="airportId()" (ngModelChange)="airportId.set($any($event))">
                <option [ngValue]="null">{{ 'booking.choose' | translate }}</option>
                @for (a of airports(); track a.id) { <option [ngValue]="a.id">{{ a.iataCode }} — {{ airportName(a) }}</option> }
              </select>
            </div>
            <div>
              <label class="field-label" for="h-area">{{ 'booking.fields.destination' | translate }}</label>
              <select id="h-area" name="areaId" class="field-input" [ngModel]="areaId()" (ngModelChange)="areaId.set($any($event))">
                <option [ngValue]="null">{{ 'booking.choose' | translate }}</option>
                @for (a of areas(); track a.id) { <option [ngValue]="a.id">{{ a.label }}</option> }
              </select>
            </div>
            <div>
              <label class="field-label" for="h-date">{{ 'booking.fields.date' | translate }}</label>
              <input id="h-date" name="date" type="date" class="field-input" [min]="today" [ngModel]="date()" (ngModelChange)="date.set($event)" />
            </div>
            <div>
              <label class="field-label" for="h-time">{{ 'booking.fields.time' | translate }}</label>
              <input id="h-time" name="time" type="time" class="field-input" [ngModel]="time()" (ngModelChange)="time.set($event)" />
            </div>
            <div class="sm:col-span-2">
              <label class="field-label" for="h-pax">{{ 'booking.fields.passengers' | translate }}</label>
              <div class="mt-1 flex items-center gap-3">
                <button type="button" class="btn-secondary w-11 px-0 text-xl" (click)="step(-1)"
                        [attr.aria-label]="'booking.fewer' | translate" [disabled]="passengers() <= 1">−</button>
                <input id="h-pax" name="passengers" type="number" min="1" [max]="maxPassengers()" inputmode="numeric"
                       class="field-input mt-0 w-20 text-center" [ngModel]="passengers()" (ngModelChange)="setPassengers($event)" />
                <button type="button" class="btn-secondary w-11 px-0 text-xl" (click)="step(1)"
                        [attr.aria-label]="'booking.more' | translate" [disabled]="passengers() >= maxPassengers()">+</button>
              </div>
            </div>
          </div>

          <div class="mt-5 rounded-2xl bg-brand-soft p-4" aria-live="polite">
            @switch (preview().state) {
              @case ('ok') {
                @if (okPreview(); as p) {
                  <p class="text-sm text-muted">{{ 'home.card.from' | translate: { vehicle: vehicleName(p.vehicle) } }}</p>
                  <p class="text-3xl font-extrabold">{{ money(p.amount, p.currency) }}</p>
                  @if (p.demo) { <p class="mt-1 text-xs font-semibold text-brand-dark">{{ 'receipt.demo' | translate }}</p> }
                }
              }
              @case ('loading') { <p class="text-sm text-muted">{{ 'home.card.loading' | translate }}</p> }
              @case ('none') { <p class="text-sm font-semibold">{{ 'home.card.none' | translate }}</p> }
              @default { <p class="text-sm text-muted">{{ 'home.card.hint' | translate }}</p> }
            }
          </div>

          <button type="submit" class="btn-primary mt-5 w-full">{{ 'home.cta' | translate }}</button>
        </form>
      </div>
    </section>

    <section class="px-4 py-14 sm:px-6 md:px-12" aria-labelledby="vehicles-title">
      <div class="mx-auto w-full max-w-6xl">
        <h2 id="vehicles-title" class="text-3xl font-extrabold">{{ 'home.vehicles.title' | translate }}</h2>
        <p class="mt-2 max-w-2xl text-muted">{{ 'home.vehicles.subtitle' | translate }}</p>
        <ul class="mt-8 grid gap-4 md:grid-cols-3">
          @for (v of vehicleTypes(); track v.id) {
            <li class="rounded-2xl border border-brand-line bg-white p-6">
              <svg viewBox="0 0 48 24" class="h-8 w-16 text-brand" fill="none" stroke="currentColor" stroke-width="2.5"
                   stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M4 17v-4l4-6h22l8 7h6v3" /><path d="M4 17h40" /><circle cx="13" cy="18" r="3" fill="white" /><circle cx="35" cy="18" r="3" fill="white" />
              </svg>
              <h3 class="mt-4 text-xl font-extrabold">{{ vehicleName(v) }}</h3>
              <p class="mt-1 text-muted">{{ 'home.vehicles.capacity' | translate: { min: v.minPassengers, max: v.maxPassengers } }}</p>
            </li>
          }
        </ul>
      </div>
    </section>

    <section class="bg-brand-soft/60 px-4 py-12 sm:px-6 md:px-12" aria-labelledby="airports-title">
      <div class="mx-auto w-full max-w-6xl">
        <h2 id="airports-title" class="text-2xl font-extrabold">{{ 'home.airports.title' | translate }}</h2>
        <ul class="mt-5 flex flex-wrap gap-3">
          @for (a of airports(); track a.id) {
            <li>
              <a routerLink="/book" [queryParams]="{ airportId: a.id }"
                 class="inline-flex min-h-11 items-center gap-2 rounded-full border border-brand-line bg-white px-4 text-sm font-semibold hover:border-brand focus:outline-none focus-visible:ring-2 focus-visible:ring-brand">
                <span class="font-extrabold text-brand">{{ a.iataCode }}</span>{{ airportName(a) }}
              </a>
            </li>
          }
        </ul>
      </div>
    </section>

    <section class="px-4 py-14 sm:px-6 md:px-12" aria-labelledby="how-title">
      <div class="mx-auto w-full max-w-6xl">
        <h2 id="how-title" class="text-3xl font-extrabold">{{ 'home.how.title' | translate }}</h2>
        <ol class="mt-8 grid gap-4 md:grid-cols-3">
          @for (n of [1, 2, 3]; track n) {
            <li class="rounded-2xl border border-brand-line bg-white p-6">
              <span class="inline-flex h-10 w-10 items-center justify-center rounded-full bg-brand font-extrabold text-white">{{ n }}</span>
              <h3 class="mt-4 text-lg font-extrabold">{{ 'home.how.s' + n + '.title' | translate }}</h3>
              <p class="mt-1 text-muted">{{ 'home.how.s' + n + '.text' | translate }}</p>
            </li>
          }
        </ol>
      </div>
    </section>

    <footer class="border-t border-brand-line bg-white px-4 py-10 sm:px-6 md:px-12">
      <div class="mx-auto flex w-full max-w-6xl flex-wrap items-start justify-between gap-6">
        <div>
          <p class="text-xl font-extrabold text-brand">Marhba</p>
          <p class="text-xs font-semibold tracking-widest text-muted">{{ 'home.footer.tagline' | translate }}</p>
        </div>
        <nav class="flex flex-wrap gap-x-6 gap-y-2 text-sm font-semibold" [attr.aria-label]="'home.footer.links' | translate">
          <a routerLink="/book" class="hover:text-brand">{{ 'nav.book' | translate }}</a>
          <a routerLink="/bookings" class="hover:text-brand">{{ 'nav.bookings' | translate }}</a>
          <a routerLink="/login" class="hover:text-brand">{{ 'auth.login.submit' | translate }}</a>
        </nav>
        <p class="w-full text-xs text-muted">{{ 'home.footer.rights' | translate: { year: year } }}</p>
      </div>
    </footer>
  `
})
export class HomeComponent {
  private readonly catalog = inject(CatalogService);
  private readonly bookings = inject(BookingService);
  private readonly currency = inject(CurrencyService);
  private readonly lang = inject(LanguageService);
  private readonly router = inject(Router);

  readonly directions: Direction[] = ['AIRPORT_TO_ADDRESS', 'ADDRESS_TO_AIRPORT'];
  readonly today = todayInTunis();
  readonly year = new Date().getFullYear();

  readonly airports = toSignal(this.catalog.airports(), { initialValue: [] as Airport[] });
  private readonly zones = toSignal(this.catalog.zones(), { initialValue: [] as ZoneWithAreas[] });
  readonly vehicleTypes = toSignal(this.catalog.vehicleTypes(), { initialValue: [] as VehicleType[] });

  readonly direction = signal<Direction>('AIRPORT_TO_ADDRESS');
  readonly airportId = signal<number | null>(null);
  readonly areaId = signal<number | null>(null);
  readonly date = signal('');
  readonly time = signal('');
  readonly passengers = signal(1);

  readonly areas = computed(() =>
    this.zones()
      .flatMap((z) => z.areas.map((a) => ({ id: a.id, zoneId: z.id, label: localName(a, this.lang.current()) })))
      .sort((a, b) => a.label.localeCompare(b.label, this.lang.current()))
  );

  readonly maxPassengers = computed(() => Math.max(1, ...this.vehicleTypes().map((v) => v.maxPassengers)));

  /** The smallest vehicle that carries the group: the price shown is the starting price for this trip. */
  private readonly vehicle = computed(() =>
    [...this.vehicleTypes()]
      .sort((a, b) => a.maxPassengers - b.maxPassengers)
      .find((v) => v.maxPassengers >= this.passengers())
  );

  private readonly quoteParams = computed(() => {
    const area = this.areas().find((a) => a.id === this.areaId());
    const airportId = this.airportId();
    const vehicle = this.vehicle();
    return airportId !== null && area && vehicle
      ? { airportId, zoneId: area.zoneId, vehicleTypeId: vehicle.id, currency: this.currency.current(), vehicle }
      : null;
  });

  /** Live price from the API (never computed in the browser). */
  readonly preview = toSignal(
    toObservable(this.quoteParams).pipe(
      switchMap((p): Observable<Preview> =>
        p === null
          ? of<Preview>({ state: 'idle' })
          : this.bookings
              .quote({ airportId: p.airportId, zoneId: p.zoneId, vehicleTypeId: p.vehicleTypeId, currency: p.currency, roundTrip: false })
              .pipe(
                map((q): Preview => ({ state: 'ok', amount: q.leg.amount, currency: q.leg.currency, vehicle: p.vehicle, demo: q.leg.demo })),
                catchError(() => of<Preview>({ state: 'none' })),
                startWith<Preview>({ state: 'loading' })
              )
      )
    ),
    { initialValue: { state: 'idle' } as Preview }
  );

  readonly okPreview = computed(() => {
    const p = this.preview();
    return p.state === 'ok' ? p : null;
  });

  airportName(item: Airport): string {
    return localName(item, this.lang.current());
  }

  vehicleName(v: VehicleType): string {
    return localName(v, this.lang.current());
  }

  money(amount: number, currency: string): string {
    return formatMoney(amount, currency, this.lang.current());
  }

  step(delta: number): void {
    this.setPassengers(this.passengers() + delta);
  }

  setPassengers(value: number | string | null): void {
    const n = Math.floor(Number(value));
    this.passengers.set(Number.isFinite(n) ? Math.min(this.maxPassengers(), Math.max(1, n)) : 1);
  }

  /** Hands the card's choices to the booking flow (which asks to sign in if needed). */
  go(): void {
    const queryParams: Record<string, string | number> = { direction: this.direction(), passengers: this.passengers() };
    if (this.airportId() !== null) {
      queryParams['airportId'] = this.airportId() as number;
    }
    if (this.areaId() !== null) {
      queryParams['areaId'] = this.areaId() as number;
    }
    if (this.date()) {
      queryParams['date'] = this.date();
    }
    if (this.time()) {
      queryParams['time'] = this.time();
    }
    void this.router.navigate(['/book'], { queryParams });
  }
}
