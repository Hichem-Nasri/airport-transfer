import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { errorCode, errorKey } from '../../core/api-error';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { formatMoney } from '../../core/money';
import { Person, StaffBooking, StaffService } from '../../core/staff.service';
import { formatTunis } from '../../core/tunis-time';
import { StatusBadgeComponent } from '../../shared/status-badge.component';

@Component({
  selector: 'app-staff-booking-detail',
  standalone: true,
  imports: [FormsModule, RouterLink, TranslateModule, StatusBadgeComponent],
  template: `
    <section class="mx-auto w-full max-w-3xl px-4 py-8 sm:px-6">
      <a routerLink="/staff/bookings" class="font-semibold text-brand hover:underline">← {{ 'staff.backToList' | translate }}</a>

      @if (loadError(); as key) {
        <p class="mt-6 rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
      }

      @if (data(); as d) {
        <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
          <h1 class="text-3xl font-extrabold tracking-wide" dir="ltr">{{ d.booking.reference }}</h1>
          <app-status-badge [status]="d.booking.status" />
        </div>
        @if (d.booking.groupId) {
          <p class="mt-1 text-sm font-semibold text-brand">{{ 'bookings.roundTrip' | translate }}</p>
        }

        <div class="mt-6 grid gap-4 sm:grid-cols-2">
          <section class="rounded-2xl border border-brand-line bg-white p-4">
            <h2 class="font-extrabold">{{ 'staff.trip' | translate }}</h2>
            <dl class="mt-3 space-y-2 text-sm">
              <div><dt class="text-muted">{{ 'booking.fields.airport' | translate }}</dt><dd class="font-semibold">{{ d.booking.airport.iataCode }} · {{ name(d.booking.airport) }}</dd></div>
              <div><dt class="text-muted">{{ 'booking.direction.' + d.booking.direction | translate }}</dt><dd class="font-semibold">{{ name(d.booking.zone) }}</dd></div>
              <div><dt class="text-muted">{{ 'booking.fields.address' | translate }}</dt><dd class="font-semibold">{{ d.booking.address }}</dd></div>
              <div><dt class="text-muted">{{ 'staff.pickup' | translate }}</dt><dd class="font-semibold">{{ when(d.booking.pickupAt) }}</dd></div>
              @if (d.booking.flightNumber) {
                <div><dt class="text-muted">{{ 'booking.fields.flightNumber' | translate }}</dt><dd class="font-semibold" dir="ltr">{{ d.booking.flightNumber }}</dd></div>
              }
              <div><dt class="text-muted">{{ 'booking.fields.passengers' | translate }} / {{ 'booking.fields.luggage' | translate }}</dt><dd class="font-semibold">{{ d.booking.passengers }} / {{ d.booking.luggageCount }} · {{ name(d.booking.vehicleType) }}</dd></div>
              @if (d.booking.notes) {
                <div><dt class="text-muted">{{ 'booking.fields.notes' | translate }}</dt><dd class="font-semibold">{{ d.booking.notes }}</dd></div>
              }
            </dl>
          </section>

          <section class="rounded-2xl border border-brand-line bg-white p-4">
            <h2 class="font-extrabold">{{ 'staff.customer' | translate }}</h2>
            @if (d.customer; as c) {
              <p class="mt-3 font-semibold">{{ c.fullName }}</p>
              <p class="text-sm"><a class="text-brand hover:underline" [href]="'mailto:' + c.email">{{ c.email }}</a></p>
              @if (c.phone) { <p class="text-sm" dir="ltr"><a class="text-brand hover:underline" [href]="'tel:' + c.phone">{{ c.phone }}</a></p> }
            }
            <h2 class="mt-5 font-extrabold">{{ 'staff.price' | translate }}</h2>
            <p class="mt-2 text-xl font-extrabold">{{ money(d) }}</p>
            <p class="text-sm text-muted">
              {{ 'booking.payment.' + d.booking.paymentMethod | translate }} ·
              <span class="font-semibold" [class.text-brand-dark]="d.booking.paymentStatus === 'UNPAID'">{{ 'payment.status.' + d.booking.paymentStatus | translate }}</span>
            </p>
            @if (d.booking.priceDemo) { <p class="mt-1 text-sm text-muted">{{ 'booking.demoNotice' | translate }}</p> }
            <h2 class="mt-5 font-extrabold">{{ 'staff.driver' | translate }}</h2>
            @if (d.driver; as dr) {
              <p class="mt-2 font-semibold">{{ dr.fullName }}</p>
              @if (dr.phone) { <p class="text-sm" dir="ltr"><a class="text-brand hover:underline" [href]="'tel:' + dr.phone">{{ dr.phone }}</a></p> }
            } @else {
              <p class="mt-2 font-semibold text-brand-dark">{{ 'staff.noDriver' | translate }}</p>
            }
          </section>
        </div>

        <section class="mt-4 rounded-2xl border border-brand-line bg-white p-4" [attr.aria-label]="'staff.actions' | translate">
          <h2 class="font-extrabold">{{ 'staff.actions' | translate }}</h2>

          @if (actionError(); as key) {
            <p class="mt-3 rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
          }

          @if (terminal()) {
            <p class="mt-3 text-muted">{{ 'staff.noActions' | translate }}</p>
          } @else {
            <div class="mt-3 flex flex-wrap gap-2">
              @if (d.booking.status === 'PENDING') {
                <button type="button" class="btn-primary" [disabled]="busy()" (click)="run(service.confirm(d.booking.id))">{{ 'staff.confirm' | translate }}</button>
              }
              @if (d.booking.status === 'ASSIGNED') {
                <button type="button" class="btn-primary" [disabled]="busy()" (click)="run(service.start(d.booking.id))">{{ 'staff.start' | translate }}</button>
              }
              @if (d.booking.status === 'IN_PROGRESS') {
                <button type="button" class="btn-primary" [disabled]="busy()" (click)="run(service.complete(d.booking.id))">{{ 'staff.complete' | translate }}</button>
              }
              @if (d.booking.paymentMethod === 'CASH' && d.booking.paymentStatus === 'UNPAID' && (d.booking.status === 'IN_PROGRESS' || d.booking.status === 'COMPLETED')) {
                <button type="button" class="btn-secondary" [disabled]="busy()" (click)="run(service.cashCollected(d.booking.id))">{{ 'driver.cashCollected' | translate }}</button>
              }
              @if (d.booking.cancellable) {
                <button type="button" class="btn-secondary" (click)="cancelling.set(!cancelling())">{{ 'bookings.cancel' | translate }}</button>
              }
            </div>

            @if (d.booking.status === 'CONFIRMED' || d.booking.status === 'ASSIGNED') {
              <div class="mt-5">
                <label class="field-label" for="driver">{{ d.booking.status === 'ASSIGNED' ? ('staff.reassign' | translate) : ('staff.assign' | translate) }}</label>
                <div class="mt-1 flex flex-col gap-2 sm:flex-row">
                  <select id="driver" class="field-input mt-0" [(ngModel)]="driverId">
                    <option [ngValue]="null" disabled>{{ 'booking.choose' | translate }}</option>
                    @for (dr of drivers(); track dr.id) {
                      <option [ngValue]="dr.id">{{ dr.fullName }}</option>
                    }
                  </select>
                  <button type="button" class="btn-primary" [disabled]="busy() || driverId() === null" (click)="assign(d, false)">
                    {{ 'staff.assignAction' | translate }}
                  </button>
                </div>
                @if (conflict()) {
                  <div class="mt-3 rounded-xl bg-brand-soft p-3" role="alert">
                    <p class="text-sm font-semibold">{{ 'errors.DRIVER_UNAVAILABLE' | translate }}</p>
                    <button type="button" class="btn-secondary mt-2" [disabled]="busy()" (click)="assign(d, true)">{{ 'staff.assignAnyway' | translate }}</button>
                  </div>
                }
              </div>
            }

            @if (cancelling()) {
              <div class="mt-5">
                <label class="field-label" for="reason">{{ 'staff.cancelReason' | translate }}</label>
                <textarea id="reason" rows="2" maxlength="500" class="field-input py-2" [(ngModel)]="reason"></textarea>
                <button type="button" class="btn-primary mt-2" [disabled]="busy()" (click)="cancel(d)">{{ 'bookings.cancelYes' | translate }}</button>
              </div>
            }
          }
        </section>

        @if (d.history; as history) {
          <section class="mt-4 rounded-2xl border border-brand-line bg-white p-4">
            <h2 class="font-extrabold">{{ 'staff.history' | translate }}</h2>
            <ol class="mt-3 space-y-3 border-s-2 border-brand-line ps-4">
              @for (h of history; track $index) {
                <li>
                  <p class="font-semibold">{{ 'status.' + h.toStatus | translate }}</p>
                  <p class="text-sm text-muted">
                    {{ when(h.createdAt) }} · {{ h.actorName }}
                    @if (h.actorRole) { ({{ 'roles.' + h.actorRole | translate }}) }
                  </p>
                  @if (h.note) { <p class="text-sm">{{ h.note }}</p> }
                </li>
              }
            </ol>
          </section>
        }
      }
    </section>
  `
})
export class StaffBookingDetailComponent {
  readonly service = inject(StaffService);
  private readonly lang = inject(LanguageService);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  readonly drivers = toSignal(this.service.drivers(), { initialValue: [] as Person[] });
  readonly data = signal<StaffBooking | null>(null);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly busy = signal(false);
  readonly conflict = signal(false);
  readonly cancelling = signal(false);
  readonly driverId = signal<number | null>(null);
  readonly reason = signal('');

  readonly terminal = computed(() => {
    const status = this.data()?.booking.status;
    return status === 'COMPLETED' || status === 'CANCELLED';
  });

  constructor() {
    this.service.get(this.id).subscribe({
      next: (d) => this.data.set(d),
      error: (e: unknown) => this.loadError.set(errorKey(e))
    });
  }

  name(item: { nameFr: string; nameEn: string; nameAr: string }): string {
    return localName(item, this.lang.current());
  }

  when(iso: string): string {
    return formatTunis(iso, this.lang.current());
  }

  money(d: StaffBooking): string {
    return formatMoney(d.booking.amount, d.booking.currency, this.lang.current());
  }

  assign(d: StaffBooking, force: boolean): void {
    const driver = this.driverId();
    if (driver !== null) {
      this.run(this.service.assign(d.booking.id, driver, force));
    }
  }

  cancel(d: StaffBooking): void {
    this.run(this.service.cancel(d.booking.id, this.reason()));
  }

  run(action: Observable<StaffBooking>): void {
    this.busy.set(true);
    this.actionError.set(null);
    this.conflict.set(false);
    action.subscribe({
      next: (d) => {
        this.data.set(d);
        this.busy.set(false);
        this.cancelling.set(false);
        this.reason.set('');
      },
      error: (e: unknown) => {
        if (errorCode(e) === 'DRIVER_UNAVAILABLE') {
          this.conflict.set(true);
        } else {
          this.actionError.set(errorKey(e));
        }
        this.busy.set(false);
      }
    });
  }
}
