import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [TranslateModule, RouterLink],
  template: `
    <section class="px-6 py-12 md:px-12">
      <span class="inline-block rounded-full bg-[#FDE3E5] px-3.5 py-1.5 text-sm font-semibold text-brand-dark">
        {{ 'home.badge' | translate }}
      </span>
      <h1 class="mt-5 max-w-xl text-4xl font-extrabold leading-tight md:text-6xl">{{ 'home.title' | translate }}</h1>
      <p class="mt-4 max-w-lg text-lg leading-relaxed text-muted">{{ 'home.subtitle' | translate }}</p>
      <a routerLink="/book" class="btn-primary mt-8">{{ 'home.cta' | translate }}</a>
      <p class="mt-8 text-sm font-semibold" [class.text-brand]="api() === 'down'">
        {{ (api() === 'up' ? 'api.up' : api() === 'down' ? 'api.down' : 'api.checking') | translate }}
      </p>
    </section>
  `
})
export class HomeComponent {
  readonly api = signal<'checking' | 'up' | 'down'>('checking');

  constructor() {
    inject(HttpClient)
      .get('/api/public/ping')
      .subscribe({ next: () => this.api.set('up'), error: () => this.api.set('down') });
  }
}
