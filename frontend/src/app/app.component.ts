import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
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
    <header class="flex flex-wrap items-center justify-between gap-4 px-6 py-5 md:px-12">
      <a routerLink="/" aria-label="Marhba"><app-logo /></a>
      <nav class="flex flex-wrap gap-7 text-[15px] font-semibold">
        <a routerLink="/book" class="hover:text-brand">{{ 'nav.book' | translate }}</a>
        @if (auth.isLoggedIn()) {
          <a routerLink="/bookings" class="hover:text-brand">{{ 'nav.bookings' | translate }}</a>
        }
        <a href="#" class="hover:text-brand">{{ 'nav.destinations' | translate }}</a>
        <a href="#" class="hover:text-brand">{{ 'nav.vehicles' | translate }}</a>
        <a href="#" class="hover:text-brand">{{ 'nav.how' | translate }}</a>
      </nav>
      <div class="flex flex-wrap items-center gap-3">
        <div class="flex rounded-full border border-brand-line bg-white p-1">
          @for (c of currency.available; track c) {
            <button
              type="button"
              class="min-h-9 rounded-full px-3.5 text-sm font-semibold"
              [class.bg-brand]="currency.current() === c"
              [class.text-white]="currency.current() === c"
              (click)="currency.set(c)"
            >{{ c }}</button>
          }
        </div>
        <select
          class="min-h-11 rounded-xl border border-brand-line bg-white px-3 text-sm font-semibold"
          aria-label="Language"
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
    <router-outlet />
  `
})
export class AppComponent {
  readonly currency = inject(CurrencyService);
  readonly lang = inject(LanguageService);
  readonly auth = inject(AuthService);

  onLang(value: string): void {
    this.lang.use(value as Lang);
  }
}
