import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { errorKey } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';

const MIN_PASSWORD = 8;

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule],
  template: `
    <section class="mx-auto w-full max-w-md px-6 py-10">
      <h1 class="text-3xl font-extrabold">{{ 'account.title' | translate }}</h1>

      @if (auth.user(); as user) {
        <dl class="mt-8 divide-y divide-brand-line rounded-2xl border border-brand-line bg-white">
          <div class="px-5 py-4">
            <dt class="text-sm text-muted">{{ 'auth.fields.email' | translate }}</dt>
            <dd class="mt-0.5 break-all font-semibold">{{ user.email }}</dd>
          </div>
          <div class="px-5 py-4">
            <dt class="text-sm text-muted">{{ 'account.role' | translate }}</dt>
            <dd class="mt-1">
              <span class="inline-block rounded-full bg-brand-soft px-3 py-1 text-sm font-semibold text-brand-dark">
                {{ 'roles.' + user.role | translate }}
              </span>
            </dd>
          </div>
        </dl>

        <h2 class="mt-10 text-xl font-extrabold">{{ 'account.profile.title' | translate }}</h2>
        <form class="mt-4 space-y-5" [formGroup]="profile" (ngSubmit)="saveProfile()" novalidate>
          <div>
            <label class="field-label" for="fullName">{{ 'auth.fields.fullName' | translate }}</label>
            <input id="fullName" class="field-input" autocomplete="name" formControlName="fullName"
                   [attr.aria-invalid]="profile.controls.fullName.touched && profile.controls.fullName.invalid" />
            @if (profile.controls.fullName.touched && profile.controls.fullName.invalid) {
              <p class="field-error">{{ 'validation.required' | translate }}</p>
            }
          </div>
          <div>
            <label class="field-label" for="phone">{{ 'auth.fields.phone' | translate }}</label>
            <input id="phone" type="tel" class="field-input" autocomplete="tel" dir="ltr" formControlName="phone"
                   [attr.aria-invalid]="profile.controls.phone.invalid" />
            @if (profile.controls.phone.invalid) {
              <p class="field-error">{{ 'validation.phone' | translate }}</p>
            }
          </div>
          @if (profileError(); as key) {
            <p class="rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
          }
          @if (profileSaved()) {
            <p class="rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold" role="status">{{ 'account.profile.saved' | translate }}</p>
          }
          <button type="submit" class="btn-primary w-full" [disabled]="profileBusy()">{{ 'admin.save' | translate }}</button>
        </form>

        <h2 class="mt-10 text-xl font-extrabold">{{ 'account.password.title' | translate }}</h2>
        <form class="mt-4 space-y-5" [formGroup]="password" (ngSubmit)="savePassword()" novalidate>
          <div>
            <label class="field-label" for="currentPassword">{{ 'account.password.current' | translate }}</label>
            <input id="currentPassword" type="password" autocomplete="current-password" class="field-input"
                   formControlName="currentPassword"
                   [attr.aria-invalid]="password.controls.currentPassword.touched && password.controls.currentPassword.invalid" />
            @if (password.controls.currentPassword.touched && password.controls.currentPassword.invalid) {
              <p class="field-error">{{ 'validation.required' | translate }}</p>
            }
          </div>
          <div>
            <label class="field-label" for="newPassword">{{ 'auth.reset.newPassword' | translate }}</label>
            <input id="newPassword" type="password" autocomplete="new-password" class="field-input"
                   formControlName="newPassword"
                   [attr.aria-invalid]="password.controls.newPassword.touched && password.controls.newPassword.invalid" />
            <p class="mt-1 text-sm text-muted">{{ 'auth.passwordHint' | translate: { min: minPassword } }}</p>
            @if (password.controls.newPassword.touched && password.controls.newPassword.invalid) {
              <p class="field-error">
                {{ (password.controls.newPassword.hasError('required') ? 'validation.required' : 'validation.minlength') | translate: { min: minPassword } }}
              </p>
            }
          </div>
          @if (passwordError(); as key) {
            <p class="rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
          }
          @if (passwordSaved()) {
            <p class="rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold" role="status">{{ 'account.password.saved' | translate }}</p>
          }
          <button type="submit" class="btn-primary w-full" [disabled]="passwordBusy()">{{ 'account.password.submit' | translate }}</button>
        </form>
      } @else {
        <p class="mt-8 text-muted" role="status">{{ 'account.loading' | translate }}</p>
      }

      <button type="button" class="btn-secondary mt-10 w-full" (click)="auth.logout()">
        {{ 'nav.logout' | translate }}
      </button>
    </section>
  `
})
export class AccountComponent {
  readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly minPassword = MIN_PASSWORD;
  readonly profile = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(120)]],
    phone: ['', [Validators.maxLength(30), Validators.pattern(/^[+0-9 ()-]*$/)]]
  });
  readonly password = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(MIN_PASSWORD), Validators.maxLength(72)]]
  });

  readonly profileBusy = signal(false);
  readonly profileSaved = signal(false);
  readonly profileError = signal<string | null>(null);
  readonly passwordBusy = signal(false);
  readonly passwordSaved = signal(false);
  readonly passwordError = signal<string | null>(null);
  private filled = false;

  constructor() {
    // The user may arrive after a page reload (restored session): fill the form once.
    effect(() => {
      const user = this.auth.user();
      if (user && !this.filled) {
        this.filled = true;
        this.profile.reset({ fullName: user.fullName, phone: user.phone ?? '' });
      }
    });
  }

  saveProfile(): void {
    if (this.profile.invalid) {
      this.profile.markAllAsTouched();
      return;
    }
    const v = this.profile.getRawValue();
    this.profileBusy.set(true);
    this.profileError.set(null);
    this.profileSaved.set(false);
    this.auth.updateProfile(v.fullName.trim(), v.phone).subscribe({
      next: () => {
        this.profileBusy.set(false);
        this.profileSaved.set(true);
      },
      error: (e: unknown) => {
        this.profileBusy.set(false);
        this.profileError.set(errorKey(e));
      }
    });
  }

  savePassword(): void {
    if (this.password.invalid) {
      this.password.markAllAsTouched();
      return;
    }
    const v = this.password.getRawValue();
    this.passwordBusy.set(true);
    this.passwordError.set(null);
    this.passwordSaved.set(false);
    this.auth.changePassword(v.currentPassword, v.newPassword).subscribe({
      next: () => {
        this.passwordBusy.set(false);
        this.passwordSaved.set(true);
        this.password.reset();
      },
      error: (e: unknown) => {
        this.passwordBusy.set(false);
        this.passwordError.set(errorKey(e));
      }
    });
  }
}
