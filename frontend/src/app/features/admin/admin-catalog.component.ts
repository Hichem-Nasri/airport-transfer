import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Observable, forkJoin } from 'rxjs';
import {
  AdminAirport,
  AdminArea,
  AdminService,
  AdminVehicleType,
  AdminZone
} from '../../core/admin.service';
import { errorKey } from '../../core/api-error';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';

type Section = 'airports' | 'zones' | 'areas' | 'vehicles';
type Row = AdminAirport | AdminZone | AdminArea | AdminVehicleType;

/** Airports, pricing zones, areas (governorates) and vehicle types. Items are deactivated, never deleted. */
@Component({
  selector: 'app-admin-catalog',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule],
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h2 class="text-2xl font-extrabold">{{ 'admin.catalog.title' | translate }}</h2>
      <button type="button" class="btn-primary" (click)="openNew()">{{ 'admin.catalog.new' | translate }}</button>
    </div>

    <div class="mt-4 flex gap-2 overflow-x-auto pb-1" role="tablist">
      @for (s of sections; track s) {
        <button type="button" role="tab" [attr.aria-selected]="section() === s"
          class="inline-flex min-h-11 shrink-0 items-center rounded-full border px-4 text-sm font-semibold"
          [class.bg-brand]="section() === s" [class.text-white]="section() === s"
          [class.border-brand]="section() === s" [class.border-brand-line]="section() !== s"
          (click)="select(s)">{{ 'admin.catalog.' + s | translate }}</button>
      }
    </div>

    @if (error(); as key) {
      <p class="mt-4 rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
    }
    @if (notice(); as n) {
      <p class="mt-4 rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold" role="status">{{ n | translate }}</p>
    }

    @if (formOpen()) {
      <form [formGroup]="form" (ngSubmit)="save()" class="mt-4 grid gap-3 rounded-2xl border border-brand bg-white p-4 sm:grid-cols-2">
        <div>
          <label class="field-label" for="c-code">{{ (section() === 'airports' ? 'admin.catalog.iata' : 'admin.catalog.code') | translate }}</label>
          <input id="c-code" class="field-input" formControlName="code" dir="ltr" />
        </div>
        <div><label class="field-label" for="c-fr">{{ 'admin.catalog.nameFr' | translate }}</label><input id="c-fr" class="field-input" formControlName="nameFr" /></div>
        <div><label class="field-label" for="c-en">{{ 'admin.catalog.nameEn' | translate }}</label><input id="c-en" class="field-input" formControlName="nameEn" /></div>
        <div><label class="field-label" for="c-ar">{{ 'admin.catalog.nameAr' | translate }}</label><input id="c-ar" class="field-input" formControlName="nameAr" dir="rtl" /></div>

        @switch (section()) {
          @case ('airports') {
            <div><label class="field-label" for="c-lat">{{ 'admin.catalog.latitude' | translate }}</label><input id="c-lat" type="number" step="any" class="field-input" formControlName="latitude" dir="ltr" /></div>
            <div><label class="field-label" for="c-lng">{{ 'admin.catalog.longitude' | translate }}</label><input id="c-lng" type="number" step="any" class="field-input" formControlName="longitude" dir="ltr" /></div>
          }
          @case ('zones') {
            <div><label class="field-label" for="c-ord">{{ 'admin.catalog.order' | translate }}</label><input id="c-ord" type="number" min="0" class="field-input" formControlName="displayOrder" /></div>
          }
          @case ('areas') {
            <div>
              <label class="field-label" for="c-zone">{{ 'admin.catalog.zone' | translate }}</label>
              <select id="c-zone" class="field-input" formControlName="zoneId">
                @for (z of zones(); track z.id) { <option [ngValue]="z.id">{{ z.code }} · {{ name(z) }}</option> }
              </select>
            </div>
          }
          @case ('vehicles') {
            <div><label class="field-label" for="c-min">{{ 'admin.catalog.minPassengers' | translate }}</label><input id="c-min" type="number" min="1" class="field-input" formControlName="minPassengers" /></div>
            <div><label class="field-label" for="c-max">{{ 'admin.catalog.maxPassengers' | translate }}</label><input id="c-max" type="number" min="1" class="field-input" formControlName="maxPassengers" /></div>
            <div><label class="field-label" for="c-ord2">{{ 'admin.catalog.order' | translate }}</label><input id="c-ord2" type="number" min="0" class="field-input" formControlName="displayOrder" /></div>
          }
        }
        <label class="flex min-h-11 items-center gap-3 font-semibold">
          <input type="checkbox" class="h-5 w-5 accent-[#D2001A]" formControlName="active" />
          {{ 'admin.catalog.active' | translate }}
        </label>
        @if (formError(); as key) {
          <p class="text-sm font-semibold text-brand-dark sm:col-span-2" role="alert">{{ key | translate }}</p>
        }
        <div class="flex gap-2 sm:col-span-2 sm:justify-end">
          <button type="button" class="btn-secondary" (click)="closeForm()">{{ 'admin.cancel' | translate }}</button>
          <button type="submit" class="btn-primary" [disabled]="saving()">{{ 'admin.save' | translate }}</button>
        </div>
      </form>
    }

    @if (loading()) {
      <p class="mt-6 text-muted" role="status">{{ 'account.loading' | translate }}</p>
    } @else {
      <ul class="mt-4 space-y-3">
        @for (row of rows(); track row.id) {
          <li class="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-line bg-white p-4" [class.opacity-60]="!row.active">
            <span>
              <span class="font-extrabold" dir="ltr">{{ codeOf(row) }}</span>
              <span class="ms-2 font-semibold">{{ name(row) }}</span>
              @if (extra(row); as e) { <span class="ms-2 text-sm text-muted">{{ e }}</span> }
              @if (!row.active) { <span class="ms-2 rounded-full border border-brand-line px-2 py-0.5 text-xs font-extrabold">{{ 'admin.catalog.inactive' | translate }}</span> }
            </span>
            <button type="button" class="btn-secondary" (click)="openEdit(row)">{{ 'admin.users.edit' | translate }}</button>
          </li>
        }
      </ul>
    }
  `
})
export class AdminCatalogComponent {
  private readonly admin = inject(AdminService);
  private readonly lang = inject(LanguageService);

  readonly sections: Section[] = ['airports', 'zones', 'areas', 'vehicles'];
  readonly section = signal<Section>('airports');
  readonly airports = signal<AdminAirport[]>([]);
  readonly zones = signal<AdminZone[]>([]);
  readonly areas = signal<AdminArea[]>([]);
  readonly vehicles = signal<AdminVehicleType[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly formOpen = signal(false);
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  private editingId: number | null = null;

  readonly form = inject(FormBuilder).nonNullable.group({
    code: ['', Validators.required],
    nameFr: ['', Validators.required],
    nameEn: ['', Validators.required],
    nameAr: ['', Validators.required],
    latitude: [null as number | null],
    longitude: [null as number | null],
    displayOrder: [0],
    zoneId: [0],
    minPassengers: [1],
    maxPassengers: [3],
    active: [true]
  });

  constructor() {
    this.load();
  }

  rows(): Row[] {
    switch (this.section()) {
      case 'airports':
        return this.airports();
      case 'zones':
        return this.zones();
      case 'areas':
        return this.areas();
      default:
        return this.vehicles();
    }
  }

  select(s: Section): void {
    this.section.set(s);
    this.closeForm();
    this.notice.set(null);
  }

  name(row: Row): string {
    return localName(row, this.lang.current());
  }

  codeOf(row: Row): string {
    return 'iataCode' in row ? row.iataCode : row.code;
  }

  /** Short secondary text: vehicle capacity or the zone of an area. */
  extra(row: Row): string {
    if ('maxPassengers' in row) {
      return `${row.minPassengers}–${row.maxPassengers}`;
    }
    if ('zoneId' in row) {
      return this.zones().find((z) => z.id === row.zoneId)?.code ?? '';
    }
    return '';
  }

  openNew(): void {
    this.editingId = null;
    this.form.reset({ displayOrder: 0, zoneId: this.zones()[0]?.id ?? 0, minPassengers: 1, maxPassengers: 3, active: true });
    this.formError.set(null);
    this.formOpen.set(true);
  }

  openEdit(row: Row): void {
    this.editingId = row.id;
    this.form.reset({
      code: this.codeOf(row),
      nameFr: row.nameFr,
      nameEn: row.nameEn,
      nameAr: row.nameAr,
      latitude: 'latitude' in row ? row.latitude : null,
      longitude: 'longitude' in row ? row.longitude : null,
      displayOrder: 'displayOrder' in row ? row.displayOrder : 0,
      zoneId: 'zoneId' in row ? row.zoneId : 0,
      minPassengers: 'minPassengers' in row ? row.minPassengers : 1,
      maxPassengers: 'maxPassengers' in row ? row.maxPassengers : 3,
      active: row.active
    });
    this.formError.set(null);
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
    this.formError.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.formError.set('validation.required');
      return;
    }
    const v = this.form.getRawValue();
    const names = { nameFr: v.nameFr.trim(), nameEn: v.nameEn.trim(), nameAr: v.nameAr.trim() };
    const id = this.editingId;
    let request: Observable<unknown>;
    switch (this.section()) {
      case 'airports':
        request = this.admin.saveAirport(id, {
          iataCode: v.code.trim().toUpperCase(), ...names, latitude: v.latitude, longitude: v.longitude, active: v.active
        });
        break;
      case 'zones':
        request = this.admin.saveZone(id, { code: v.code.trim(), ...names, displayOrder: v.displayOrder, active: v.active });
        break;
      case 'areas':
        request = this.admin.saveArea(id, { code: v.code.trim(), ...names, zoneId: v.zoneId, active: v.active });
        break;
      default:
        request = this.admin.saveVehicleType(id, {
          code: v.code.trim(), ...names, minPassengers: v.minPassengers, maxPassengers: v.maxPassengers,
          displayOrder: v.displayOrder, active: v.active
        });
    }
    this.saving.set(true);
    this.formError.set(null);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.notice.set('admin.catalog.saved');
        this.closeForm();
        this.load();
      },
      error: (e: unknown) => {
        this.saving.set(false);
        this.formError.set(errorKey(e));
      }
    });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      airports: this.admin.airports(),
      zones: this.admin.zones(),
      areas: this.admin.areas(),
      vehicles: this.admin.vehicleTypes()
    }).subscribe({
      next: (r) => {
        this.airports.set(r.airports);
        this.zones.set([...r.zones].sort((a, b) => a.displayOrder - b.displayOrder));
        this.areas.set(r.areas);
        this.vehicles.set([...r.vehicles].sort((a, b) => a.displayOrder - b.displayOrder));
        this.loading.set(false);
      },
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }
}
