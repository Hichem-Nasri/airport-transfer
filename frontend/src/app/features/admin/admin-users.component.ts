import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { AdminService, AdminUser } from '../../core/admin.service';
import { errorKey } from '../../core/api-error';
import { Role } from '../../core/auth.service';
import { LanguageService } from '../../core/language.service';
import { Page } from '../../core/staff.service';
import { formatTunis } from '../../core/tunis-time';

const ROLES: Role[] = ['CUSTOMER', 'DRIVER', 'MODERATOR', 'ADMIN'];
const PAGE_SIZE = 20;

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule],
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h2 class="text-2xl font-extrabold">{{ 'admin.users.title' | translate }}</h2>
      <button type="button" class="btn-primary" (click)="openCreate()">{{ 'admin.users.new' | translate }}</button>
    </div>

    <form [formGroup]="filters" (ngSubmit)="go(0)" class="mt-4 grid gap-3 rounded-2xl border border-brand-line bg-white p-4 sm:grid-cols-4">
      <div class="sm:col-span-2">
        <label class="field-label" for="uq">{{ 'staff.filters.search' | translate }}</label>
        <input id="uq" type="search" class="field-input" formControlName="q" [placeholder]="'admin.users.searchHint' | translate" />
      </div>
      <div>
        <label class="field-label" for="urole">{{ 'admin.users.role' | translate }}</label>
        <select id="urole" class="field-input" formControlName="role">
          <option value="">{{ 'staff.filters.all' | translate }}</option>
          @for (r of roles; track r) { <option [value]="r">{{ 'roles.' + r | translate }}</option> }
        </select>
      </div>
      <div>
        <label class="field-label" for="uen">{{ 'admin.users.state' | translate }}</label>
        <select id="uen" class="field-input" formControlName="enabled">
          <option [ngValue]="null">{{ 'staff.filters.all' | translate }}</option>
          <option [ngValue]="true">{{ 'admin.users.enabled' | translate }}</option>
          <option [ngValue]="false">{{ 'admin.users.disabled' | translate }}</option>
        </select>
      </div>
      <div class="sm:col-span-4 sm:text-end"><button type="submit" class="btn-primary">{{ 'staff.filters.apply' | translate }}</button></div>
    </form>

    @if (error(); as key) {
      <p class="mt-4 rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
    }
    @if (notice(); as n) {
      <p class="mt-4 rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold" role="status">{{ n | translate }}</p>
    }

    @if (editing() !== null || creating()) {
      <form [formGroup]="form" (ngSubmit)="save()" class="mt-4 grid gap-3 rounded-2xl border border-brand bg-white p-4 sm:grid-cols-2">
        <h3 class="text-lg font-extrabold sm:col-span-2">
          {{ (creating() ? 'admin.users.new' : 'admin.users.edit') | translate }}
        </h3>
        <div>
          <label class="field-label" for="fe">{{ 'auth.fields.email' | translate }}</label>
          <input id="fe" type="email" class="field-input" formControlName="email" autocomplete="off" />
        </div>
        @if (creating()) {
          <div>
            <label class="field-label" for="fp">{{ 'admin.users.password' | translate }}</label>
            <input id="fp" type="password" class="field-input" formControlName="password" autocomplete="new-password" />
          </div>
        }
        <div>
          <label class="field-label" for="fn">{{ 'auth.fields.fullName' | translate }}</label>
          <input id="fn" class="field-input" formControlName="fullName" />
        </div>
        <div>
          <label class="field-label" for="fph">{{ 'auth.fields.phone' | translate }}</label>
          <input id="fph" type="tel" class="field-input" formControlName="phone" dir="ltr" />
        </div>
        <div>
          <label class="field-label" for="fr">{{ 'admin.users.role' | translate }}</label>
          <select id="fr" class="field-input" formControlName="role">
            @for (r of roles; track r) { <option [value]="r">{{ 'roles.' + r | translate }}</option> }
          </select>
        </div>
        @if (!creating()) {
          <label class="flex min-h-11 items-center gap-3 font-semibold">
            <input type="checkbox" class="h-5 w-5 accent-[#D2001A]" formControlName="enabled" />
            {{ 'admin.users.enabled' | translate }}
          </label>
        }
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
    } @else if (page(); as p) {
      <p class="mt-4 text-sm font-semibold text-muted" role="status">{{ 'admin.users.count' | translate: { count: p.totalElements } }}</p>
      <ul class="mt-3 space-y-3">
        @for (u of p.content; track u.id) {
          <li class="rounded-2xl border border-brand-line bg-white p-4" [class.opacity-60]="!u.enabled">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <span class="text-lg font-extrabold">{{ u.fullName }}</span>
              <span class="flex gap-2 text-xs font-extrabold">
                <span class="rounded-full bg-brand-soft px-3 py-1 text-brand-dark">{{ 'roles.' + u.role | translate }}</span>
                @if (!u.enabled) {
                  <span class="rounded-full border border-brand-line px-3 py-1">{{ 'admin.users.disabled' | translate }}</span>
                }
              </span>
            </div>
            <p class="mt-1 text-sm" dir="ltr">{{ u.email }}@if (u.phone) { · {{ u.phone }} }</p>
            <p class="text-xs text-muted">{{ 'admin.users.since' | translate: { date: when(u.createdAt) } }}</p>
            <div class="mt-3">
              <button type="button" class="btn-secondary" (click)="openEdit(u)">{{ 'admin.users.edit' | translate }}</button>
            </div>
          </li>
        }
      </ul>
      @if (p.totalPages > 1) {
        <nav class="mt-6 flex items-center justify-between gap-3" [attr.aria-label]="'staff.pagination' | translate">
          <button type="button" class="btn-secondary" [disabled]="p.page === 0" (click)="go(p.page - 1)">‹ {{ 'staff.previous' | translate }}</button>
          <span class="text-sm font-semibold">{{ p.page + 1 }} / {{ p.totalPages }}</span>
          <button type="button" class="btn-secondary" [disabled]="p.page + 1 >= p.totalPages" (click)="go(p.page + 1)">{{ 'staff.next' | translate }} ›</button>
        </nav>
      }
    }
  `
})
export class AdminUsersComponent {
  private readonly admin = inject(AdminService);
  private readonly lang = inject(LanguageService);

  readonly roles = ROLES;
  readonly filters = inject(FormBuilder).nonNullable.group({
    q: [''],
    role: ['' as Role | ''],
    enabled: [null as boolean | null]
  });
  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    fullName: ['', [Validators.required, Validators.maxLength(120)]],
    phone: [''],
    role: ['DRIVER' as Role],
    enabled: [true]
  });

  readonly page = signal<Page<AdminUser> | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly creating = signal(false);
  readonly editing = signal<AdminUser | null>(null);
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  private index = 0;

  constructor() {
    this.load();
  }

  go(i: number): void {
    this.index = i;
    this.load();
  }

  when(iso: string): string {
    return formatTunis(iso, this.lang.current());
  }

  openCreate(): void {
    this.closeForm();
    this.form.reset({ role: 'DRIVER', enabled: true });
    this.form.controls.email.enable();
    this.form.controls.password.setValidators([Validators.required, Validators.minLength(8), Validators.maxLength(72)]);
    this.form.controls.password.updateValueAndValidity();
    this.creating.set(true);
  }

  openEdit(u: AdminUser): void {
    this.closeForm();
    this.form.reset({ email: u.email, password: '', fullName: u.fullName, phone: u.phone ?? '', role: u.role, enabled: u.enabled });
    this.form.controls.email.disable();
    this.form.controls.password.clearValidators();
    this.form.controls.password.updateValueAndValidity();
    this.editing.set(u);
  }

  closeForm(): void {
    this.creating.set(false);
    this.editing.set(null);
    this.formError.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.formError.set('validation.required');
      return;
    }
    const v = this.form.getRawValue();
    const editing = this.editing();
    this.saving.set(true);
    this.formError.set(null);
    const request =
      editing === null
        ? this.admin.createUser({ email: v.email, password: v.password, fullName: v.fullName, phone: v.phone, role: v.role })
        : this.admin.updateUser(editing.id, { fullName: v.fullName, phone: v.phone, role: v.role, enabled: v.enabled });
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.notice.set(editing === null ? 'admin.users.created' : 'admin.users.updated');
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
    const f = this.filters.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.admin.users({ q: f.q, role: f.role, enabled: f.enabled, page: this.index, size: PAGE_SIZE }).subscribe({
      next: (p) => {
        this.page.set(p);
        this.loading.set(false);
      },
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }
}
