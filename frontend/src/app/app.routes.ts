import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent) },
  { path: 'login', loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent) },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'account',
    canActivate: [authGuard],
    loadComponent: () => import('./features/auth/account.component').then((m) => m.AccountComponent)
  },
  {
    path: 'book',
    canActivate: [authGuard],
    loadComponent: () => import('./features/booking/booking-flow.component').then((m) => m.BookingFlowComponent)
  },
  {
    path: 'bookings',
    canActivate: [authGuard],
    loadComponent: () => import('./features/booking/bookings.component').then((m) => m.BookingsComponent)
  },
  { path: '**', redirectTo: '' }
];
