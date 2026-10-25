import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { errorKey, safeReturnUrl } from '../../core/api-error';
import { AuthService, homeFor } from '../../core/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  template: `
    <section class="mx-auto w-full max-w-md px-6 py-10">
      <h1 class="text-3xl font-extrabold">{{ 'auth.register.title' | translate }}</h1>
      <p class="mt-2 text-muted">{{ 'auth.register.subtitle' | translate }}</p>

      <form class="mt-8 space-y-5" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div>
          <label class="field-label" for="fullName">{{ 'auth.fields.fullName' | translate }}</label>
          <input id="fullName" type="text" autocomplete="name" class="field-input" formControlName="fullName"
                 [attr.aria-invalid]="form.controls.fullName.touched && form.controls.fullName.invalid"
                 aria-describedby="fullName-error" />
          @if (form.controls.fullName.touched && form.controls.fullName.invalid) {
            <p id="fullName-error" class="field-error">{{ 'validation.required' | translate }}</p>
          }
        </div>
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
          <label class="field-label" for="phone">{{ 'auth.fields.phone' | translate }}</label>
          <input id="phone" type="tel" autocomplete="tel" class="field-input" formControlName="phone"
                 [attr.aria-invalid]="form.controls.phone.touched && form.controls.phone.invalid"
                 aria-describedby="phone-error" />
          @if (form.controls.phone.touched && form.controls.phone.invalid) {
            <p id="phone-error" class="field-error">{{ 'validation.phone' | translate }}</p>
          }
        </div>
        <div>
          <label class="field-label" for="password">{{ 'auth.fields.password' | translate }}</label>
          <input id="password" type="password" autocomplete="new-password" class="field-input"
                 formControlName="password"
                 [attr.aria-invalid]="form.controls.password.touched && form.controls.password.invalid"
                 aria-describedby="password-hint password-error" />
          <p id="password-hint" class="mt-1 text-sm text-muted">{{ 'auth.passwordHint' | translate: { min: 8 } }}</p>
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <p id="password-error" class="field-error">
              {{ (form.controls.password.hasError('required') ? 'validation.required' : 'validation.minlength')
                 | translate: { min: 8 } }}
            </p>
          }
        </div>

        @if (error(); as key) {
          <p class="rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">
            {{ key | translate }}
          </p>
        }

        <button type="submit" class="btn-primary w-full" [disabled]="loading()">
          {{ 'auth.register.submit' | translate }}
        </button>
      </form>

      <p class="mt-6 text-sm text-muted">
        {{ 'auth.register.haveAccount' | translate }}
        <a routerLink="/login" [queryParams]="returnUrlParams" class="font-semibold text-brand hover:underline">
          {{ 'auth.register.login' | translate }}
        </a>
      </p>
    </section>
  `
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly form = inject(FormBuilder).nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.pattern(/^[+0-9 ()-]*$/), Validators.maxLength(30)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]]
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
    this.auth.register(this.form.getRawValue()).subscribe({
      next: (response) =>
        void this.router.navigateByUrl(safeReturnUrl(this.returnUrl, homeFor(response.user.role))),
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }
}
