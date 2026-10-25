import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { errorKey, safeReturnUrl } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  template: `
    <section class="mx-auto w-full max-w-md px-6 py-10">
      <h1 class="text-3xl font-extrabold">{{ 'auth.login.title' | translate }}</h1>
      <p class="mt-2 text-muted">{{ 'auth.login.subtitle' | translate }}</p>

      <form class="mt-8 space-y-5" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div>
          <label class="field-label" for="email">{{ 'auth.fields.email' | translate }}</label>
          <input id="email" type="email" autocomplete="email" class="field-input" formControlName="email"
                 [attr.aria-invalid]="form.controls.email.touched && form.controls.email.invalid"
                 aria-describedby="email-error" />
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <p id="email-error" class="field-error">
              {{ (form.controls.email.hasError('required') ? 'validation.required' : 'validation.email') | translate }}
            </p>
          }
        </div>
        <div>
          <label class="field-label" for="password">{{ 'auth.fields.password' | translate }}</label>
          <input id="password" type="password" autocomplete="current-password" class="field-input"
                 formControlName="password"
                 [attr.aria-invalid]="form.controls.password.touched && form.controls.password.invalid"
                 aria-describedby="password-error" />
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <p id="password-error" class="field-error">{{ 'validation.required' | translate }}</p>
          }
        </div>

        @if (error(); as key) {
          <p class="rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">
            {{ key | translate }}
          </p>
        }

        <button type="submit" class="btn-primary w-full" [disabled]="loading()">
          {{ 'auth.login.submit' | translate }}
        </button>
      </form>

      <p class="mt-6 text-sm text-muted">
        {{ 'auth.login.noAccount' | translate }}
        <a routerLink="/register" [queryParams]="returnUrlParams" class="font-semibold text-brand hover:underline">
          {{ 'auth.login.register' | translate }}
        </a>
      </p>
    </section>
  `
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  private readonly returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
  readonly returnUrlParams = this.returnUrl ? { returnUrl: this.returnUrl } : {};

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl(safeReturnUrl(this.returnUrl)),
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }
}
