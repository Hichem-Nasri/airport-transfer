import { Component, inject } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [TranslateModule],
  template: `
    <section class="mx-auto w-full max-w-md px-6 py-10">
      <h1 class="text-3xl font-extrabold">{{ 'account.title' | translate }}</h1>

      @if (auth.user(); as user) {
        <dl class="mt-8 divide-y divide-brand-line rounded-2xl border border-brand-line bg-white">
          <div class="px-5 py-4">
            <dt class="text-sm text-muted">{{ 'auth.fields.fullName' | translate }}</dt>
            <dd class="mt-0.5 font-semibold">{{ user.fullName }}</dd>
          </div>
          <div class="px-5 py-4">
            <dt class="text-sm text-muted">{{ 'auth.fields.email' | translate }}</dt>
            <dd class="mt-0.5 break-all font-semibold">{{ user.email }}</dd>
          </div>
          @if (user.phone) {
            <div class="px-5 py-4">
              <dt class="text-sm text-muted">{{ 'auth.fields.phone' | translate }}</dt>
              <dd class="mt-0.5 font-semibold" dir="ltr">{{ user.phone }}</dd>
            </div>
          }
          <div class="px-5 py-4">
            <dt class="text-sm text-muted">{{ 'account.role' | translate }}</dt>
            <dd class="mt-1">
              <span class="inline-block rounded-full bg-brand-soft px-3 py-1 text-sm font-semibold text-brand-dark">
                {{ 'roles.' + user.role | translate }}
              </span>
            </dd>
          </div>
        </dl>
      } @else {
        <p class="mt-8 text-muted" role="status">{{ 'account.loading' | translate }}</p>
      }

      <button type="button" class="btn-secondary mt-8 w-full" (click)="auth.logout()">
        {{ 'nav.logout' | translate }}
      </button>
    </section>
  `
})
export class AccountComponent {
  readonly auth = inject(AuthService);
}
