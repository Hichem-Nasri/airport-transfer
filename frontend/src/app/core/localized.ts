import { Lang } from './language.service';

export interface Localized {
  nameFr: string;
  nameEn: string;
  nameAr: string;
}

/** Picks the name in the active language. */
export function localName(item: Localized, lang: Lang): string {
  switch (lang) {
    case 'ar':
      return item.nameAr;
    case 'en':
      return item.nameEn;
    default:
      return item.nameFr;
  }
}
