import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from './core/auth.service';
import { CurrencyService } from './core/currency.service';
import { Lang, LanguageService } from './core/language.service';
import { LogoComponent } from './shared/logo.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, TranslateModule, LogoComponent],
  template: `
    <a href="#main" class="skip-link" (click)="skipToMain($event)">{{ 'a11y.skip' | translate }}</a>
    <header class="flex flex-wrap print:hidden items-center justify-between gap-4 px-6 py-5 md:px-12">
      <a routerLink="/" aria-label="Marhba"><app-logo /></a>
      <nav class="flex flex-wrap gap-7 text-[15px] font-semibold" [attr.aria-label]="'a11y.mainNav' | translate">
        <a routerLink="/book" class="hover:text-brand">{{ 'nav.book' | translate }}</a>
        @if (auth.isLoggedIn()) {
          <a routerLink="/bookings" class="hover:text-brand">{{ 'nav.bookings' | translate }}</a>
        }
        @if (auth.hasRole('MODERATOR', 'ADMIN')) {
          <a routerLink="/staff/bookings" class="hover:text-brand">{{ 'nav.staff' | translate }}</a>
        }
        @if (auth.hasRole('ADMIN')) {
          <a routerLink="/admin" class="hover:text-brand">{{ 'nav.admin' | translate }}</a>
        }
        @if (auth.hasRole('DRIVER')) {
          <a routerLink="/driver" class="hover:text-brand">{{ 'nav.driver' | translate }}</a>
        }
        <a href="#" class="hover:text-brand">{{ 'nav.destinations' | translate }}</a>
        <a href="#" class="hover:text-brand">{{ 'nav.vehicles' | translate }}</a>
        <a href="#" class="hover:text-brand">{{ 'nav.how' | translate }}</a>
      </nav>
      <div class="flex flex-wrap items-center gap-3">
        <div class="flex rounded-full border border-brand-line bg-white p-1" role="group" [attr.aria-label]="'a11y.currency' | translate">
          @for (c of currency.available; track c) {
            <button
              type="button"
              class="min-h-9 rounded-full px-3.5 text-sm font-semibold"
              [class.bg-brand]="currency.current() === c"
              [class.text-white]="currency.current() === c"
              [attr.aria-pressed]="currency.current() === c"
              (click)="currency.set(c)"
            >{{ c }}</button>
          }
        </div>
        <select
          class="min-h-11 rounded-xl border border-brand-line bg-white px-3 text-sm font-semibold"
          [attr.aria-label]="'a11y.language' | translate"
          (change)="onLang($any($event.target).value)"
        >
          @for (l of lang.available; track l) {
            <option [value]="l" [selected]="lang.current() === l">{{ l.toUpperCase() }}</option>
          }
        </select>
        @if (auth.isLoggedIn()) {
          <a routerLink="/account" class="btn-secondary">{{ 'nav.account' | translate }}</a>
        } @else {
          <a routerLink="/login" class="btn-primary">{{ 'nav.login' | translate }}</a>
        }
      </div>
    </header>
    <main id="main" tabindex="-1" class="outline-none"><router-outlet /></main>
  `
})
export class AppComponent {
  readonly currency = inject(CurrencyService);
  readonly lang = inject(LanguageService);
  readonly auth = inject(AuthService);

  constructor() {
    // Move focus to the page content after each navigation so keyboard and screen-reader users start at the top.
    inject(Router).events.pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed()).subscribe(() => {
      document.getElementById('main')?.focus({ preventScroll: true });
    });
  }

  skipToMain(event: Event): void {
    event.preventDefault();
    document.getElementById('main')?.focus();
  }

  onLang(value: string): void {
    this.lang.use(value as Lang);
  }
}
