import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, of, shareReplay, tap } from 'rxjs';

export type Role = 'CUSTOMER' | 'DRIVER' | 'MODERATOR' | 'ADMIN';

/** Where each role lands after signing in. */
export function homeFor(role: Role): string {
  switch (role) {
    case 'ADMIN':
    case 'MODERATOR':
      return '/staff/bookings';
    case 'DRIVER':
      return '/driver';
    default:
      return '/bookings';
  }
}

export interface User {
  id: number;
  email: string;
  fullName: string;
  phone: string | null;
  role: Role;
  createdAt: string;
}

export interface Credentials {
  email: string;
  password: string;
}

export interface RegisterPayload extends Credentials {
  fullName: string;
  phone?: string;
}

interface AuthResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

const STORAGE_KEY = 'marhba.auth';

interface StoredSession {
  token: string;
  expiresAt: string;
}

/** Holds the JWT and the current user. The token is kept in localStorage (wrapped in try/catch: it can be blocked). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokenSignal = signal<string | null>(null);
  readonly token = this.tokenSignal.asReadonly();
  readonly user = signal<User | null>(null);
  readonly isLoggedIn = computed(() => this.tokenSignal() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  /** Emits once the user of a restored session is known (null when the session is not valid). */
  private readonly restored$: Observable<User | null>;

  constructor() {
    const stored = this.readStorage();
    if (stored) {
      this.tokenSignal.set(stored.token);
      this.restored$ = this.http.get<User>('/api/auth/me').pipe(
        tap((user) => this.user.set(user)),
        // A 401 is handled by the interceptor (logout); other errors keep the session.
        catchError(() => of(null)),
        shareReplay(1)
      );
      this.restored$.subscribe();
    } else {
      this.restored$ = of(null);
    }
  }

  /** The signed-in user, waiting for a restored session to finish loading. */
  whenUserKnown(): Observable<User | null> {
    const user = this.user();
    return user ? of(user) : this.restored$;
  }

  hasRole(...roles: Role[]): boolean {
    const role = this.user()?.role;
    return role !== undefined && roles.includes(role);
  }

  login(credentials: Credentials): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', credentials).pipe(tap((r) => this.start(r)));
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', payload).pipe(tap((r) => this.start(r)));
  }

  logout(redirect = true): void {
    this.tokenSignal.set(null);
    this.user.set(null);
    this.writeStorage(null);
    if (redirect) {
      void this.router.navigateByUrl('/');
    }
  }

  private start(response: AuthResponse): void {
    this.tokenSignal.set(response.token);
    this.user.set(response.user);
    this.writeStorage({ token: response.token, expiresAt: response.expiresAt });
  }

  private readStorage(): StoredSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as StoredSession;
      if (!session.token || Date.parse(session.expiresAt) <= Date.now()) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return session;
    } catch {
      return null;
    }
  }

  private writeStorage(session: StoredSession | null): void {
    try {
      if (session) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
      } else {
        localStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      // Storage unavailable: the session simply lasts until the page is closed.
    }
  }
}
