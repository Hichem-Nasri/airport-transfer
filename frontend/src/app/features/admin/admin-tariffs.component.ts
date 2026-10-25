import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin, of } from 'rxjs';
import {
  AdminAirport,
  AdminService,
  AdminTariff,
  AdminVehicleType,
  AdminZone
} from '../../core/admin.service';
import { errorKey } from '../../core/api-error';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { parseTndInput } from '../../core/tariff-input';

@Component({
  selector: 'app-admin-tariffs',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  template: `
    <h2 class="text-2xl font-extrabold">{{ 'admin.tariffs.title' | translate }}</h2>
    <p class="mt-1 text-sm text-muted">{{ 'admin.tariffs.hint' | translate }}</p>

    <div class="mt-4 max-w-sm">
      <label class="field-label" for="tairport">{{ 'booking.fields.airport' | translate }}</label>
      <select id="tairport" class="field-input" [ngModel]="airportId()" (ngModelChange)="selectAirport($any($event))">
        <option [ngValue]="null" disabled>{{ 'admin.tariffs.pickAirport' | translate }}</option>
        @for (a of airports(); track a.id) {
          <option [ngValue]="a.id">{{ a.iataCode }} — {{ airportName(a) }}</option>
        }
      </select>
    </div>

    @if (error(); as key) {
      <p class="mt-4 rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
    }
    @if (notice(); as n) {
      <p class="mt-4 rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold" role="status">{{ n | translate }}</p>
    }

    @if (airportId() !== null) {
      @if (loading()) {
        <p class="mt-6 text-muted" role="status">{{ 'account.loading' | translate }}</p>
      } @else {
        <ul class="mt-6 space-y-3">
          @for (z of zones(); track z.id) {
            <li class="rounded-2xl border border-brand-line bg-white p-4" [class.opacity-60]="!z.active">
              <h3 class="font-extrabold">{{ z.code }} · {{ zoneName(z) }}</h3>
              <div class="mt-3 grid gap-3 sm:grid-cols-3">
                @for (v of vehicleTypes(); track v.id) {
                  <div>
                    <label class="field-label" [for]="'p-' + z.id + '-' + v.id">{{ vehicleName(v) }} (TND)</label>
                    <input
                      [id]="'p-' + z.id + '-' + v.id"
                      class="field-input"
                      inputmode="decimal"
                      dir="ltr"
                      [value]="draft(z.id, v.id)"
                      (input)="setDraft(z.id, v.id, $any($event.target).value)"
                      [attr.aria-invalid]="isInvalid(z.id, v.id)"
                    />
                    @if (tariff(z.id, v.id); as t) {
                      @if (t.demo) {
                        <span class="mt-1 inline-block rounded-full bg-brand-soft px-2 py-0.5 text-xs font-extrabold text-brand-dark">{{ 'admin.tariffs.demo' | translate }}</span>
                      }
                    } @else {
                      <span class="mt-1 inline-block text-xs text-muted">{{ 'admin.tariffs.missing' | translate }}</span>
                    }
                  </div>
                }
              </div>
              <div class="mt-3 sm:text-end">
                <button type="button" class="btn-primary" [disabled]="saving() || !rowDirty(z.id)" (click)="saveRow(z.id)">
                  {{ 'admin.save' | translate }}
                </button>
              </div>
            </li>
          }
        </ul>
      }
    }
  `
})
export class AdminTariffsComponent {
  private readonly admin = inject(AdminService);
  private readonly lang = inject(LanguageService);

  readonly airports = toSignal(this.admin.airports(), { initialValue: [] as AdminAirport[] });
  readonly zones = signal<AdminZone[]>([]);
  readonly vehicleTypes = signal<AdminVehicleType[]>([]);
  readonly airportId = signal<number | null>(null);
  readonly tariffs = signal<AdminTariff[]>([]);
  readonly drafts = signal<Record<string, string>>({});
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);

  private readonly byKey = computed(() => {
    const map = new Map<string, AdminTariff>();
    for (const t of this.tariffs()) {
      map.set(this.key(t.zoneId, t.vehicleTypeId), t);
    }
    return map;
  });

  airportName(a: AdminAirport): string {
    return localName(a, this.lang.current());
  }
  zoneName(z: AdminZone): string {
    return localName(z, this.lang.current());
  }
  vehicleName(v: AdminVehicleType): string {
    return localName(v, this.lang.current());
  }

  selectAirport(id: number | null): void {
    this.airportId.set(id);
    this.notice.set(null);
    if (id !== null) {
      this.load(id);
    }
  }

  tariff(zoneId: number, vehicleTypeId: number): AdminTariff | undefined {
    return this.byKey().get(this.key(zoneId, vehicleTypeId));
  }

  draft(zoneId: number, vehicleTypeId: number): string {
    const k = this.key(zoneId, vehicleTypeId);
    const d = this.drafts()[k];
    if (d !== undefined) {
      return d;
    }
    const t = this.byKey().get(k);
    return t ? this.plain(t.priceTnd) : '';
  }

  setDraft(zoneId: number, vehicleTypeId: number, value: string): void {
    this.drafts.update((d) => ({ ...d, [this.key(zoneId, vehicleTypeId)]: value }));
  }

  isInvalid(zoneId: number, vehicleTypeId: number): boolean {
    const k = this.key(zoneId, vehicleTypeId);
    const d = this.drafts()[k];
    return d !== undefined && d.trim() !== '' && parseTndInput(d) === null;
  }

  rowDirty(zoneId: number): boolean {
    return this.vehicleTypes().some((v) => this.changed(zoneId, v.id) !== null);
  }

  saveRow(zoneId: number): void {
    const airportId = this.airportId();
    if (airportId === null) {
      return;
    }
    if (this.vehicleTypes().some((v) => this.isInvalid(zoneId, v.id))) {
      this.error.set('admin.tariffs.invalidPrice');
      return;
    }
    const calls = this.vehicleTypes().flatMap((v) => {
      const price = this.changed(zoneId, v.id);
      if (price === null) {
        return [];
      }
      const existing = this.tariff(zoneId, v.id);
      const body = { airportId, zoneId, vehicleTypeId: v.id, priceTnd: price, active: existing?.active ?? true };
      return [existing ? this.admin.updateTariff(existing.id, body) : this.admin.createTariff(body)];
    });
    this.saving.set(true);
    this.error.set(null);
    this.notice.set(null);
    (calls.length ? forkJoin(calls) : of([])).subscribe({
      next: () => {
        this.saving.set(false);
        this.notice.set('admin.tariffs.saved');
        this.drafts.update((d) => {
          const next = { ...d };
          this.vehicleTypes().forEach((v) => delete next[this.key(zoneId, v.id)]);
          return next;
        });
        this.load(airportId);
      },
      error: (e: unknown) => {
        this.saving.set(false);
        this.error.set(errorKey(e));
        this.load(airportId);
      }
    });
  }

  /** The new price of a cell when it is valid and differs from the saved one; otherwise null. */
  private changed(zoneId: number, vehicleTypeId: number): number | null {
    const d = this.drafts()[this.key(zoneId, vehicleTypeId)];
    if (d === undefined) {
      return null;
    }
    const parsed = parseTndInput(d);
    const existing = this.tariff(zoneId, vehicleTypeId);
    if (parsed === null) {
      return null;
    }
    // A demo tariff re-saved with the same number becomes a real one, so it counts as a change.
    return existing && !existing.demo && existing.priceTnd === parsed ? null : parsed;
  }

  private load(airportId: number): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      zones: this.admin.zones(),
      vehicleTypes: this.admin.vehicleTypes(),
      tariffs: this.admin.tariffs(airportId)
    }).subscribe({
      next: (r) => {
        this.zones.set([...r.zones].sort((a, b) => a.displayOrder - b.displayOrder));
        this.vehicleTypes.set([...r.vehicleTypes].sort((a, b) => a.displayOrder - b.displayOrder));
        this.tariffs.set(r.tariffs);
        this.loading.set(false);
      },
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }

  private key(zoneId: number, vehicleTypeId: number): string {
    return `${zoneId}-${vehicleTypeId}`;
  }

  /** Shows a saved price as typed text without trailing zeros (display only). */
  private plain(price: number): string {
    return String(price);
  }
}
