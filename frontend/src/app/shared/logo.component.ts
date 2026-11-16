import { Component } from '@angular/core';

@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <div class="flex items-center gap-3">
      <svg width="44" height="44" viewBox="0 0 48 48" aria-hidden="true" class="shrink-0">
        <rect width="48" height="48" rx="13" fill="#D2001A" />
        <path d="M9 35C15 19 27 13 40 15" fill="none" stroke="#fff" stroke-width="3.5" stroke-linecap="round" />
        <path d="M33 9.5 40.5 15 33.5 21" fill="none" stroke="#fff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" />
        <circle cx="10" cy="35" r="4.2" fill="#fff" />
        <circle cx="10" cy="35" r="1.7" fill="#D2001A" />
      </svg>
      <div dir="ltr" class="text-start">
        <div class="text-[22px] font-extrabold leading-none tracking-tight">HMS <span class="text-brand">Transport</span></div>
        <div class="mt-1 text-[10.5px] font-semibold tracking-[0.18em] text-muted">FRANCE · TUNISIE</div>
      </div>
    </div>
  `
})
export class LogoComponent {}
