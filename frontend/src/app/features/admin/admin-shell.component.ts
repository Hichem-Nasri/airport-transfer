import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslateModule],
  template: `
    <section class="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6">
      <h1 class="text-3xl font-extrabold">{{ 'admin.title' | translate }}</h1>
      <nav class="mt-5 flex gap-2 overflow-x-auto pb-1" [attr.aria-label]="'admin.title' | translate">
        @for (item of items; track item.path) {
          <a
            [routerLink]="item.path"
            routerLinkActive="bg-brand text-white border-brand"
            class="inline-flex min-h-11 shrink-0 items-center rounded-full border border-brand-line bg-white px-4 text-sm font-semibold focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
            >{{ 'admin.nav.' + item.key | translate }}</a
          >
        }
      </nav>
      <div class="mt-6"><router-outlet /></div>
    </section>
  `
})
export class AdminShellComponent {
  readonly items = [
    { path: 'stats', key: 'stats' },
    { path: 'users', key: 'users' },
    { path: 'tariffs', key: 'tariffs' },
    { path: 'catalog', key: 'catalog' },
    { path: 'currencies', key: 'currencies' }
  ];
}
