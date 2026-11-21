import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const PUBLIC_AUTH_URLS = ['/api/auth/login', '/api/auth/register'];
const PUBLIC_PREFIX = '/api/public/';

/**
 * Adds the bearer token to API calls; an expired or invalid token (401) logs the user out. Public endpoints never get
 * the token: the server rejects an expired token even where no sign-in is needed, which emptied the airport list.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  const isApi = req.url.startsWith('/api/');
  const isPublicAuth = PUBLIC_AUTH_URLS.includes(req.url) || req.url.startsWith(PUBLIC_PREFIX);

  const outgoing =
    token && isApi && !isPublicAuth ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && token && isApi && !isPublicAuth) {
        auth.logout(false);
        void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
      }
      return throwError(() => error);
    })
  );
};
