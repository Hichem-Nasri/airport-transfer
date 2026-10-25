import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { AdminCurrency, AdminRate, AdminService } from '../../core/admin.service';
import { errorKey } from '../../core/api-error';
import { LanguageService } from '../../core/language.service';
import { parseRateInput } from '../../core/tariff-input';
import { formatTunis } from '../../core/tunis-time';

/** Currencies and the append-only history of exchange rates (TND per 1 unit; the latest row applies). */
@Component({
  selector: 'app-admin-currencies',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule],
  template: `
    <h2 class="text-2xl font-extrabold">{{ 'admin.currencies.title' | translate }}</h2>
    <p class="mt-1 text-sm text-muted">{{ 'admin.currencies.hint' | translate }}</p>

    @if (error(); as key) {
      <p class="mt-4 rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
    }
    @if (notice(); as n) {
      <p class="mt-4 rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold" role="status">{{ n | translate }}</p>
    }

    @if (loading()) {
      <p class="mt-6 text-muted" role="status">{{ 'account.loading' | translate }}</p>
    } @else {
      <ul class="mt-4 space-y-3">
        @for (c of currencies(); track c.code) {
          <li class="rounded-2xl border border-brand-line bg-white p-4" [class.opacity-60]="!c.active">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <span>
                <span class="text-lg font-extrabold">{{ c.code }}</span>
                <span class="ms-2 font-semibold">{{ c.name }} ({{ c.symbol }}) · {{ c.decimals }} {{ 'admin.currencies.decimals' | translate }}</span>
                @if (c.code !== 'TND') {
                  <span class="ms-2 text-sm">
                    {{ 'admin.currencies.current' | translate }} :
                    <strong dir="ltr">{{ latest(c.code) ?? '—' }}</strong>
                  </span>
                }
              </span>
              <span class="flex gap-2">
                <button type="button" class="btn-secondary" (click)="editCurrency(c)">{{ 'admin.users.edit' | translate }}</button>
                @if (c.code !== 'TND') {
                  <button type="button" class="btn-secondary" (click)="openRate(c.code)">{{ 'admin.currencies.newRate' | translate }}</button>
                }
              </span>
            </div>
          </li>
        }
      </ul>
      <button type="button" class="btn-secondary mt-3" (click)="newCurrency()">{{ 'admin.currencies.new' | translate }}</button>
    }

    @if (currencyOpen()) {
      <form [formGroup]="currencyForm" (ngSubmit)="saveCurrency()" class="mt-4 grid gap-3 rounded-2xl border border-brand bg-white p-4 sm:grid-cols-2">
        <div>
          <label class="field-label" for="cc-code">{{ 'admin.catalog.code' | translate }}</label>
          <input id="cc-code" class="field-input" formControlName="code" dir="ltr" maxlength="3" />
        </div>
        <div><label class="field-label" for="cc-name">{{ 'admin.currencies.name' | translate }}</label><input id="cc-name" class="field-input" formControlName="name" /></div>
        <div><label class="field-label" for="cc-sym">{{ 'admin.currencies.symbol' | translate }}</label><input id="cc-sym" class="field-input" formControlName="symbol" /></div>
        <div><label class="field-label" for="cc-dec">{{ 'admin.currencies.decimals' | translate }}</label><input id="cc-dec" type="number" min="0" max="3" class="field-input" formControlName="decimals" /></div>
        <label class="flex min-h-11 items-center gap-3 font-semibold">
          <input type="checkbox" class="h-5 w-5 accent-[#D2001A]" formControlName="active" />
          {{ 'admin.catalog.active' | translate }}
        </label>
        @if (currencyError(); as key) {
          <p class="text-sm font-semibold text-brand-dark sm:col-span-2" role="alert">{{ key | translate }}</p>
        }
        <div class="flex gap-2 sm:col-span-2 sm:justify-end">
          <button type="button" class="btn-secondary" (click)="currencyOpen.set(false)">{{ 'admin.cancel' | translate }}</button>
          <button type="submit" class="btn-primary" [disabled]="saving()">{{ 'admin.save' | translate }}</button>
        </div>
      </form>
    }

    @if (rateCurrency(); as code) {
      <form [formGroup]="rateForm" (ngSubmit)="saveRate(code)" class="mt-4 grid gap-3 rounded-2xl border border-brand bg-white p-4">
        <h3 class="text-lg font-extrabold">{{ 'admin.currencies.newRate' | translate }} — {{ code }}</h3>
        <div>
          <label class="field-label" for="rate-value">{{ 'admin.currencies.rateLabel' | translate: { currency: code } }}</label>
          <input id="rate-value" class="field-input" inputmode="decimal" dir="ltr" formControlName="rate" />
          <p class="mt-1 text-xs text-muted">{{ 'admin.currencies.rateHint' | translate }}</p>
        </div>
        @if (rateError(); as key) {
          <p class="text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
        }
        <div class="flex gap-2 sm:justify-end">
          <button type="button" class="btn-secondary" (click)="rateCurrency.set(null)">{{ 'admin.cancel' | translate }}</button>
          <button type="submit" class="btn-primary" [disabled]="saving()">{{ 'admin.save' | translate }}</button>
        </div>
      </form>
    }

    @if (!loading() && rates().length > 0) {
      <h3 class="mt-8 text-xl font-extrabold">{{ 'admin.currencies.history' | translate }}</h3>
      <div class="mt-3 overflow-x-auto rounded-2xl border border-brand-line bg-white">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-brand-line text-muted">
              <th scope="col" class="p-3 text-start">{{ 'admin.stats.currency' | translate }}</th>
              <th scope="col" class="p-3 text-start">{{ 'admin.currencies.tndPerUnit' | translate }}</th>
              <th scope="col" class="p-3 text-start">{{ 'admin.currencies.date' | translate }}</th>
            </tr>
          </thead>
          <tbody>
            @for (r of rates(); track r.id) {
              <tr class="border-b border-brand-line last:border-0">
                <th scope="row" class="p-3 text-start font-extrabold">{{ r.currencyCode }}</th>
                <td class="p-3" dir="ltr">
                  {{ r.tndPerUnit }}
                  @if (r.demo) { <span class="ms-2 rounded-full bg-brand-soft px-2 py-0.5 text-xs font-extrabold text-brand-dark">{{ 'admin.tariffs.demo' | translate }}</span> }
                </td>
                <td class="p-3">{{ when(r.createdAt) }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `
})
export class AdminCurrenciesComponent {
  private readonly admin = inject(AdminService);
  private readonly lang = inject(LanguageService);
  private readonly fb = inject(FormBuilder);

  readonly currencies = signal<AdminCurrency[]>([]);
  readonly rates = signal<AdminRate[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);

  readonly currencyOpen = signal(false);
  readonly currencyError = signal<string | null>(null);
  readonly rateCurrency = signal<string | null>(null);
  readonly rateError = signal<string | null>(null);
  private currencyExists = false;

  readonly currencyForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]],
    name: ['', Validators.required],
    symbol: ['', Validators.required],
    decimals: [2, [Validators.min(0), Validators.max(3)]],
    active: [true]
  });
  readonly rateForm = this.fb.nonNullable.group({ rate: ['', Validators.required] });

  constructor() {
    this.load();
  }

  /** The newest rate of a currency, as the API returned it (the list is newest first). */
  latest(code: string): number | null {
    return this.rates().find((r) => r.currencyCode === code)?.tndPerUnit ?? null;
  }

  when(iso: string): string {
    return formatTunis(iso, this.lang.current());
  }

  newCurrency(): void {
    this.currencyExists = false;
    this.currencyForm.reset({ decimals: 2, active: true });
    this.currencyForm.controls.code.enable();
    this.currencyError.set(null);
    this.currencyOpen.set(true);
  }

  editCurrency(c: AdminCurrency): void {
    this.currencyExists = true;
    this.currencyForm.reset({ ...c });
    this.currencyForm.controls.code.disable();
    this.currencyError.set(null);
    this.currencyOpen.set(true);
  }

  saveCurrency(): void {
    if (this.currencyForm.invalid) {
      this.currencyForm.markAllAsTouched();
      this.currencyError.set('validation.required');
      return;
    }
    const v = this.currencyForm.getRawValue();
    this.saving.set(true);
    this.admin
      .saveCurrency(this.currencyExists, {
        code: v.code.trim().toUpperCase(), name: v.name.trim(), symbol: v.symbol.trim(), decimals: v.decimals, active: v.active
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.currencyOpen.set(false);
          this.notice.set('admin.currencies.saved');
          this.load();
        },
        error: (e: unknown) => {
          this.saving.set(false);
          this.currencyError.set(errorKey(e));
        }
      });
  }

  openRate(code: string): void {
    this.rateForm.reset({ rate: '' });
    this.rateError.set(null);
    this.rateCurrency.set(code);
  }

  saveRate(code: string): void {
    const rate = parseRateInput(this.rateForm.getRawValue().rate);
    if (rate === null) {
      this.rateError.set('admin.currencies.invalidRate');
      return;
    }
    this.saving.set(true);
    this.admin.addRate(code, rate).subscribe({
      next: () => {
        this.saving.set(false);
        this.rateCurrency.set(null);
        this.notice.set('admin.currencies.rateSaved');
        this.load();
      },
      error: (e: unknown) => {
        this.saving.set(false);
        this.rateError.set(errorKey(e));
      }
    });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({ currencies: this.admin.currencies(), rates: this.admin.rates() }).subscribe({
      next: (r) => {
        this.currencies.set(r.currencies);
        this.rates.set([...r.rates].sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id));
        this.loading.set(false);
      },
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }
}
