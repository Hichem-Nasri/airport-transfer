import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { errorKey } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  template: `
    <section class="mx-auto w-full max-w-md px-6 py-10">
      <h1 class="text-3xl font-extrabold">{{ 'auth.forgot.title' | translate }}</h1>
      <p class="mt-2 text-muted">{{ 'auth.forgot.subtitle' | translate }}</p>

      @if (sent()) {
        <p class="mt-8 rounded-xl bg-brand-soft px-4 py-3 font-semibold" role="status">{{ 'auth.forgot.sent' | translate }}</p>
      } @else {
        <form class="mt-8 space-y-5" [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div>
            <label class="field-label" for="email">{{ 'auth.fields.email' | translate }}</label>
            <input id="email" type="email" autocomplete="email" class="field-input" formControlName="email"
                   [attr.aria-invalid]="form.controls.email.touched && form.controls.email.invalid" />
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <p class="field-error">
                {{ (form.controls.email.hasError('required') ? 'validation.required' : 'validation.email') | translate }}
              </p>
            }
          </div>
          @if (error(); as key) {
            <p class="rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
          }
          <button type="submit" class="btn-primary w-full" [disabled]="loading()">{{ 'auth.forgot.submit' | translate }}</button>
        </form>
      }

      <p class="mt-6 text-sm">
        <a routerLink="/login" class="font-semibold text-brand hover:underline">{{ 'auth.forgot.back' | translate }}</a>
      </p>
    </section>
  `
})
export class ForgotPasswordComponent {
  private readonly auth = inject(AuthService);

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });
  readonly loading = signal(false);
  readonly sent = signal(false);
  readonly error = signal<string | null>(null);

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.forgotPassword(this.form.getRawValue().email.trim()).subscribe({
      next: () => {
        this.loading.set(false);
        this.sent.set(true);
      },
      error: (e: unknown) => {
        this.loading.set(false);
        this.error.set(errorKey(e));
      }
    });
  }
}
