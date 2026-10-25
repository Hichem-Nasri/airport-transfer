import { Component } from '@angular/core';

@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <div class="flex items-center gap-2.5">
      <svg width="40" height="44" viewBox="0 0 44 46" aria-hidden="true">
        <path d="M6 42V21a16 16 0 0 1 32 0v21z" fill="#D2001A" />
        <rect x="15" y="13" width="14" height="14" fill="#fff" />
        <rect x="15" y="13" width="14" height="14" fill="#fff" transform="rotate(45 22 20)" />
        <circle cx="22" cy="20" r="3" fill="#D2001A" />
        <rect x="4" y="42" width="36" height="3" fill="#8E0012" />
      </svg>
      <div>
        <div class="text-[22px] font-extrabold leading-none">Marhba</div>
        <div class="mt-0.5 text-[11px] font-semibold tracking-[0.12em] text-brand">TRANSFERT AÉROPORT</div>
      </div>
    </div>
  `
})
export class LogoComponent {}
