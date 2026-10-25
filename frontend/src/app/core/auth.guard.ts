import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService, Role } from './auth.service';

/** Redirects anonymous visitors to the login page, remembering where they wanted to go. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/**
 * Allows only the given roles. Anonymous visitors go to the login page; a signed-in user with another role goes
 * home. The server enforces the same rules: this guard only keeps people away from screens that would fail.
 */
export const roleGuard = (...roles: Role[]): CanActivateFn => (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  return auth.whenUserKnown().pipe(
    map((user) => (user && roles.includes(user.role) ? true : router.createUrlTree(['/'])))
  );
};
