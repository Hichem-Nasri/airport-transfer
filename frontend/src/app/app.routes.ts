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
  {
    path: 'admin',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () => import('./features/admin/admin-shell.component').then((m) => m.AdminShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'stats' },
      {
        path: 'stats',
        loadComponent: () => import('./features/admin/admin-stats.component').then((m) => m.AdminStatsComponent)
      },
      {
        path: 'users',
        loadComponent: () => import('./features/admin/admin-users.component').then((m) => m.AdminUsersComponent)
      },
      {
        path: 'tariffs',
        loadComponent: () => import('./features/admin/admin-tariffs.component').then((m) => m.AdminTariffsComponent)
      },
      {
        path: 'catalog',
        loadComponent: () => import('./features/admin/admin-catalog.component').then((m) => m.AdminCatalogComponent)
      },
      {
        path: 'currencies',
        loadComponent: () =>
          import('./features/admin/admin-currencies.component').then((m) => m.AdminCurrenciesComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
