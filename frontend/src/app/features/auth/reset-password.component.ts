import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { errorKey } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';

const MIN_PASSWORD = 8;

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  template: `
    <section class="mx-auto w-full max-w-md px-6 py-10">
      <h1 class="text-3xl font-extrabold">{{ 'auth.reset.title' | translate }}</h1>

      @if (done()) {
        <p class="mt-8 rounded-xl bg-brand-soft px-4 py-3 font-semibold" role="status">{{ 'auth.reset.done' | translate }}</p>
        <a routerLink="/login" class="btn-primary mt-6">{{ 'auth.login.submit' | translate }}</a>
      } @else if (!token) {
        <p class="mt-8 rounded-xl bg-brand-soft px-4 py-3 font-semibold text-brand-dark" role="alert">{{ 'errors.RESET_TOKEN_INVALID' | translate }}</p>
        <a routerLink="/forgot-password" class="btn-secondary mt-6">{{ 'auth.reset.again' | translate }}</a>
      } @else {
        <form class="mt-8 space-y-5" [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div>
            <label class="field-label" for="newPassword">{{ 'auth.reset.newPassword' | translate }}</label>
            <input id="newPassword" type="password" autocomplete="new-password" class="field-input"
                   formControlName="newPassword"
                   [attr.aria-invalid]="form.controls.newPassword.touched && form.controls.newPassword.invalid" />
            <p class="mt-1 text-sm text-muted">{{ 'auth.passwordHint' | translate: { min: minPassword } }}</p>
            @if (form.controls.newPassword.touched && form.controls.newPassword.invalid) {
              <p class="field-error">
                {{ (form.controls.newPassword.hasError('required') ? 'validation.required' : 'validation.minlength') | translate: { min: minPassword } }}
              </p>
            }
          </div>
          @if (error(); as key) {
            <p class="rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
            @if (tokenExpired()) {
              <a routerLink="/forgot-password" class="btn-secondary w-full">{{ 'auth.reset.again' | translate }}</a>
            }
          }
          <button type="submit" class="btn-primary w-full" [disabled]="loading()">{{ 'auth.reset.submit' | translate }}</button>
        </form>
      }
    </section>
  `
})
export class ResetPasswordComponent {
  private readonly auth = inject(AuthService);

  readonly minPassword = MIN_PASSWORD;
  readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');
  readonly form = inject(FormBuilder).nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(MIN_PASSWORD), Validators.maxLength(72)]]
  });
  readonly loading = signal(false);
  readonly done = signal(false);
  readonly error = signal<string | null>(null);
  readonly tokenExpired = signal(false);

  submit(): void {
    if (this.form.invalid || !this.token) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.resetPassword(this.token, this.form.getRawValue().newPassword).subscribe({
      next: () => {
        this.loading.set(false);
        this.done.set(true);
      },
      error: (e: unknown) => {
        this.loading.set(false);
        const key = errorKey(e);
        this.error.set(key);
        this.tokenExpired.set(key === 'errors.RESET_TOKEN_INVALID');
      }
    });
  }
}
