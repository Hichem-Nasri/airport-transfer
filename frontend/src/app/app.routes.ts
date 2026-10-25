import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth.guard';

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
  { path: 'staff', pathMatch: 'full', redirectTo: 'staff/bookings' },
  {
    path: 'staff/bookings',
    canActivate: [roleGuard('MODERATOR', 'ADMIN')],
    loadComponent: () => import('./features/staff/staff-bookings.component').then((m) => m.StaffBookingsComponent)
  },
  {
    path: 'staff/bookings/:id',
    canActivate: [roleGuard('MODERATOR', 'ADMIN')],
    loadComponent: () =>
      import('./features/staff/staff-booking-detail.component').then((m) => m.StaffBookingDetailComponent)
  },
  {
    path: 'driver',
    canActivate: [roleGuard('DRIVER')],
    loadComponent: () => import('./features/driver/driver-trips.component').then((m) => m.DriverTripsComponent)
  },
  { path: '**', redirectTo: '' }
];
