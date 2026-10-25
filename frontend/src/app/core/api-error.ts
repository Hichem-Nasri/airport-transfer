import { HttpErrorResponse } from '@angular/common/http';

/** Returns the ngx-translate key of an API error. The backend sends a stable `code`; we translate `errors.<code>`. */
export function errorKey(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const code = (error.error as { code?: string } | null)?.code;
    if (code) {
      return `errors.${code}`;
    }
    if (error.status === 0) {
      return 'errors.NETWORK';
    }
  }
  return 'errors.UNKNOWN';
}

/** Only accepts in-app paths, so a crafted `returnUrl` cannot redirect to another site. */
export function safeReturnUrl(value: string | null): string {
  return value && value.startsWith('/') && !value.startsWith('//') ? value : '/account';
}
