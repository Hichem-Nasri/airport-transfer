import { Lang } from './language.service';

const LOCALES: Record<Lang, string> = { fr: 'fr-FR', en: 'en-GB', ar: 'ar-TN' };

/** TND has 3 decimals (millimes); EUR and USD have 2. */
export function currencyDecimals(currency: string): number {
  return currency === 'TND' ? 3 : 2;
}

/**
 * Display formatting only. Amounts always come from the API already converted and rounded;
 * the browser never converts or recomputes money.
 */
export function formatMoney(amount: number, currency: string, lang: Lang): string {
  const decimals = currencyDecimals(currency);
  return new Intl.NumberFormat(LOCALES[lang], {
    style: 'currency',
    currency,
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  }).format(amount);
}
