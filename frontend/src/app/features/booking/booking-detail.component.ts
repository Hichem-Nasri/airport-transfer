import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { errorKey } from '../../core/api-error';
import { Booking, BookingService } from '../../core/booking.service';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { formatMoney } from '../../core/money';
import { formatTunis, isoToTunisParts, todayInTunis, tunisLocalToIso } from '../../core/tunis-time';
import { StatusBadgeComponent } from '../../shared/status-badge.component';

/** One booking: details, edit while PENDING or CONFIRMED, cancel, and a printable receipt. */
@Component({
  selector: 'app-booking-detail',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslateModule, StatusBadgeComponent],
  template: `
    <section class="mx-auto w-full max-w-2xl px-4 py-8 sm:px-6">
      <a routerLink="/bookings" class="text-sm font-semibold text-brand hover:underline print:hidden">‹ {{ 'bookings.title' | translate }}</a>

      @if (error(); as key) {
        <p class="mt-6 rounded-xl bg-white px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
      }

      @if (loading()) {
        <p class="mt-8 text-muted" role="status">{{ 'account.loading' | translate }}</p>
      } @else {
      @if (booking(); as b) {
        <article class="mt-4 rounded-2xl border border-brand-line bg-white p-5 print:border-0 print:p-0">
          <div class="hidden print:block">
            <p class="text-2xl font-extrabold text-brand">Marhba</p>
            <p class="text-sm font-semibold">{{ 'receipt.title' | translate }}</p>
            <p class="text-sm text-muted">{{ 'receipt.issued' | translate: { date: issued() } }}</p>
          </div>

          <div class="flex flex-wrap items-center justify-between gap-2">
            <h1 class="text-2xl font-extrabold tracking-wide" dir="ltr">{{ b.reference }}</h1>
            <app-status-badge [status]="b.status" />
          </div>
          @if (b.groupId) {
            <p class="mt-1 text-sm font-semibold text-brand">{{ 'bookings.roundTrip' | translate }}</p>
          }

          <p class="mt-4 flex flex-wrap items-center gap-x-2 text-lg font-semibold">
            <span>{{ from(b) }}</span>
            <span aria-hidden="true" class="text-brand rtl:rotate-180">→</span>
            <span>{{ to(b) }}</span>
          </p>
          <p class="text-muted">{{ when(b.pickupAt) }}</p>

          <dl class="mt-4 grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
            <dt class="text-muted">{{ 'booking.fields.address' | translate }}</dt>
            <dd class="font-semibold">{{ b.address }}</dd>
            <dt class="text-muted">{{ 'booking.fields.passengers' | translate }}</dt>
            <dd class="font-semibold">{{ b.passengers }} · {{ vehicle(b) }}</dd>
            <dt class="text-muted">{{ 'booking.fields.luggage' | translate }}</dt>
            <dd class="font-semibold">{{ b.luggageCount }}</dd>
            @if (b.flightNumber) {
              <dt class="text-muted">{{ 'booking.fields.flightNumber' | translate }}</dt>
              <dd class="font-semibold" dir="ltr">{{ b.flightNumber }}</dd>
            }
            @if (b.notes) {
              <dt class="text-muted">{{ 'booking.fields.notes' | translate }}</dt>
              <dd class="font-semibold">{{ b.notes }}</dd>
            }
            <dt class="text-muted">{{ 'booking.fields.paymentMethod' | translate }}</dt>
            <dd class="font-semibold">{{ 'booking.payment.' + b.paymentMethod | translate }}</dd>
            <dt class="text-muted">{{ 'payment.label' | translate }}</dt>
            <dd class="font-semibold">{{ 'payment.status.' + b.paymentStatus | translate }}</dd>
          </dl>

          <div class="mt-5 rounded-xl bg-brand-soft p-4">
            <p class="text-sm text-muted">{{ 'receipt.total' | translate }}</p>
            <p class="text-3xl font-extrabold">{{ money(b) }}</p>
            @if (b.currency !== 'TND') {
              <p class="mt-1 text-sm text-muted">{{ 'receipt.baseTnd' | translate: { amount: tnd(b) } }}</p>
            }
            @if (b.paymentMethod === 'CASH' && b.paymentStatus === 'UNPAID') {
              <p class="mt-2 text-sm font-semibold">{{ 'receipt.cashNote' | translate }}</p>
            }
            @if (b.priceDemo) {
              <p class="mt-2 text-xs font-semibold text-brand-dark">{{ 'receipt.demo' | translate }}</p>
            }
          </div>
        </article>

        @if (saved()) {
          <p class="mt-4 rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold print:hidden" role="status">{{ 'booking.edit.saved' | translate }}</p>
        }

        @if (editing()) {
          <form [formGroup]="form" (ngSubmit)="save(b)" novalidate class="mt-6 space-y-5 rounded-2xl border border-brand bg-white p-5 print:hidden">
            <h2 class="text-xl font-extrabold">{{ 'booking.edit.title' | translate }}</h2>
            <p class="text-sm text-muted">{{ 'booking.edit.hint' | translate }}</p>
            <div>
              <label class="field-label" for="address">{{ 'booking.fields.address' | translate }}</label>
              <input id="address" class="field-input" formControlName="address"
                     [attr.aria-invalid]="form.controls.address.touched && form.controls.address.invalid" />
              @if (form.controls.address.touched && form.controls.address.invalid) {
                <p class="field-error">{{ 'validation.required' | translate }}</p>
              }
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="field-label" for="date">{{ 'booking.fields.date' | translate }}</label>
                <input id="date" type="date" class="field-input" formControlName="date" [min]="today" />
              </div>
              <div>
                <label class="field-label" for="time">{{ 'booking.fields.time' | translate }}</label>
                <input id="time" type="time" class="field-input" formControlName="time" />
              </div>
            </div>
            @if (form.touched && form.hasError('pickupPast')) {
              <p class="field-error" role="alert">{{ 'validation.future' | translate }}</p>
            }
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="field-label" for="flight">{{ 'booking.fields.flightNumber' | translate }}</label>
                <input id="flight" class="field-input" dir="ltr" formControlName="flightNumber"
                       [attr.aria-invalid]="form.controls.flightNumber.invalid" />
                @if (form.controls.flightNumber.invalid) {
                  <p class="field-error">{{ 'validation.flight' | translate }}</p>
                }
              </div>
              <div>
                <label class="field-label" for="luggage">{{ 'booking.fields.luggage' | translate }}</label>
                <input id="luggage" type="number" min="0" max="100" class="field-input" formControlName="luggageCount" />
              </div>
            </div>
            <div>
              <label class="field-label" for="notes">{{ 'booking.fields.notes' | translate }}</label>
              <textarea id="notes" rows="3" class="field-input py-2" formControlName="notes"></textarea>
            </div>
            @if (formError(); as key) {
              <p class="rounded-xl bg-brand-soft px-4 py-3 text-sm font-semibold text-brand-dark" role="alert">{{ key | translate }}</p>
            }
            <div class="flex gap-2">
              <button type="submit" class="btn-primary" [disabled]="busy()">{{ 'admin.save' | translate }}</button>
              <button type="button" class="btn-secondary" (click)="editing.set(false)">{{ 'admin.cancel' | translate }}</button>
            </div>
          </form>
        }

        <div class="mt-6 flex flex-wrap gap-2 print:hidden">
          <button type="button" class="btn-secondary" (click)="print()">{{ 'receipt.print' | translate }}</button>
          @if (b.editable && !editing()) {
            <button type="button" class="btn-secondary" (click)="startEdit(b)">{{ 'booking.edit.open' | translate }}</button>
          }
          @if (b.cancellable && !confirming()) {
            <button type="button" class="btn-secondary" (click)="confirming.set(true)">{{ 'bookings.cancel' | translate }}</button>
          }
        </div>

        @if (confirming()) {
          <div class="mt-3 rounded-xl bg-brand-soft p-3 print:hidden" role="alertdialog" [attr.aria-label]="'bookings.cancelConfirm' | translate">
            <p class="text-sm font-semibold">{{ 'bookings.cancelConfirm' | translate }}</p>
            <div class="mt-2 flex gap-2">
              <button type="button" class="btn-primary" [disabled]="busy()" (click)="cancel(b)">{{ 'bookings.cancelYes' | translate }}</button>
              <button type="button" class="btn-secondary" (click)="confirming.set(false)">{{ 'bookings.cancelNo' | translate }}</button>
            </div>
          </div>
        }
        @if (!b.editable && b.status === 'ASSIGNED') {
          <p class="mt-3 text-sm text-muted print:hidden">{{ 'booking.edit.locked' | translate }}</p>
        }
      }
      }
    </section>
  `
})
export class BookingDetailComponent {
  private readonly service = inject(BookingService);
  private readonly lang = inject(LanguageService);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  readonly today = todayInTunis();
  readonly booking = signal<Booking | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly editing = signal(false);
  readonly confirming = signal(false);
  readonly busy = signal(false);
  readonly saved = signal(false);
  readonly formError = signal<string | null>(null);

  readonly form = inject(FormBuilder).nonNullable.group(
    {
      address: ['', [Validators.required, Validators.maxLength(500)]],
      date: ['', Validators.required],
      time: ['', Validators.required],
      flightNumber: ['', Validators.pattern(/^$|^[A-Za-z0-9 ]{2,10}$/)],
      luggageCount: [0, [Validators.min(0), Validators.max(100)]],
      notes: ['', Validators.maxLength(1000)]
    },
    {
      validators: (g) => {
        const v = g.value as { date: string; time: string };
        return v.date && v.time && Date.parse(tunisLocalToIso(v.date, v.time)) <= Date.now() ? { pickupPast: true } : null;
      }
    }
  );

  constructor() {
    this.service.get(this.id).subscribe({
      next: (b) => {
        this.booking.set(b);
        this.loading.set(false);
      },
      error: (e: unknown) => {
        this.error.set(errorKey(e));
        this.loading.set(false);
      }
    });
  }

  startEdit(b: Booking): void {
    const parts = isoToTunisParts(b.pickupAt);
    this.form.reset({
      address: b.address,
      date: parts.date,
      time: parts.time,
      flightNumber: b.flightNumber ?? '',
      luggageCount: b.luggageCount,
      notes: b.notes ?? ''
    });
    this.formError.set(null);
    this.saved.set(false);
    this.editing.set(true);
  }

  save(b: Booking): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.formError.set(null);
    this.service
      .update(b.id, {
        address: v.address.trim(),
        flightNumber: v.flightNumber.trim(),
        pickupAt: tunisLocalToIso(v.date, v.time),
        luggageCount: v.luggageCount,
        notes: v.notes.trim()
      })
      .subscribe({
        next: (updated) => {
          this.booking.set(updated);
          this.busy.set(false);
          this.editing.set(false);
          this.saved.set(true);
        },
        error: (e: unknown) => {
          this.busy.set(false);
          this.formError.set(errorKey(e));
        }
      });
  }

  cancel(b: Booking): void {
    this.busy.set(true);
    this.error.set(null);
    this.service.cancel(b.id).subscribe({
      next: (updated) => {
        this.booking.set(updated);
        this.busy.set(false);
        this.confirming.set(false);
        this.editing.set(false);
      },
      error: (e: unknown) => {
        this.busy.set(false);
        this.confirming.set(false);
        this.error.set(errorKey(e));
      }
    });
  }

  print(): void {
    window.print();
  }

  from(b: Booking): string {
    return b.direction === 'AIRPORT_TO_ADDRESS' ? this.airport(b) : localName(b.zone, this.lang.current());
  }

  to(b: Booking): string {
    return b.direction === 'AIRPORT_TO_ADDRESS' ? localName(b.zone, this.lang.current()) : this.airport(b);
  }

  vehicle(b: Booking): string {
    return localName(b.vehicleType, this.lang.current());
  }

  when(iso: string): string {
    return formatTunis(iso, this.lang.current());
  }

  issued(): string {
    return formatTunis(new Date().toISOString(), this.lang.current());
  }

  money(b: Booking): string {
    return formatMoney(b.amount, b.currency, this.lang.current());
  }

  tnd(b: Booking): string {
    return formatMoney(b.priceTnd, 'TND', this.lang.current());
  }

  private airport(b: Booking): string {
    return `${b.airport.iataCode} — ${localName(b.airport, this.lang.current())}`;
  }
}
