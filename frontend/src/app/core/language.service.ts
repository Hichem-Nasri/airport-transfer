import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type Lang = 'fr' | 'ar' | 'en';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  readonly available: Lang[] = ['fr', 'ar', 'en'];
  readonly current = signal<Lang>('fr');

  constructor() {
    this.translate.addLangs(this.available);
    this.use('fr');
  }

  use(lang: Lang): void {
    this.translate.use(lang);
    this.current.set(lang);
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
  }
}
