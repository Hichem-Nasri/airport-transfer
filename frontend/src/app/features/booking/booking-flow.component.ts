import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from '../../core/language.service';
import { formatMoney } from '../../core/money';
import { formatTunis } from '../../core/tunis-time';
import { StepIndicatorComponent } from '../../shared/step-indicator.component';
import { BookingDraftService } from './booking-draft.service';
import { DetailsStepComponent } from './details-step.component';
import { TripStepComponent } from './trip-step.component';
import { VehicleStepComponent } from './vehicle-step.component';

/** The 3-step booking flow: trip → vehicle and price → details and confirmation. */
@Component({
  selector: 'app-booking-flow',
  standalone: true,
  imports: [RouterLink, TranslateModule, StepIndicatorComponent, TripStepComponent, VehicleStepComponent, DetailsStepComponent],
  providers: [BookingDraftService],
  template: `
    <section class="mx-auto w-full max-w-2xl px-4 py-8 sm:px-6">
      @if (draft.created(); as created) {
        <div class="rounded-2xl border border-brand-line bg-white p-6" role="status">
          <h1 class="text-2xl font-extrabold">{{ 'booking.done.title' | translate }}</h1>
          <p class="mt-2 text-muted">{{ 'booking.done.text' | translate }}</p>
          <ul class="mt-5 space-y-3">
            @for (b of created.bookings; track b.id) {
              <li class="rounded-xl bg-brand-soft p-4">
                <p class="text-sm text-muted">{{ 'booking.direction.' + b.direction | translate }}</p>
                <p class="text-xl font-extrabold tracking-wide" dir="ltr">{{ b.reference }}</p>
                <p class="text-sm">{{ when(b.pickupAt) }} · {{ money(b.amount, b.currency) }}</p>
              </li>
            }
          </ul>
          <div class="mt-6 flex flex-col gap-3 sm:flex-row">
            <a routerLink="/bookings" class="btn-primary">{{ 'booking.done.viewAll' | translate }}</a>
            <button type="button" class="btn-secondary" (click)="another()">{{ 'booking.done.another' | translate }}</button>
          </div>
        </div>
      } @else {
        <h1 class="sr-only">{{ 'booking.title' | translate }}</h1>
        <app-step-indicator class="block" [labels]="labels" [current]="draft.step()" />
        <div class="mt-6">
          @switch (draft.step()) {
            @case (1) { <app-trip-step /> }
            @case (2) { <app-vehicle-step /> }
            @case (3) { <app-details-step /> }
          }
        </div>
      }
    </section>
  `
})
export class BookingFlowComponent {
  readonly draft = inject(BookingDraftService);
  private readonly lang = inject(LanguageService);

  readonly labels = ['booking.steps.trip', 'booking.steps.vehicle', 'booking.steps.details'];

  when(iso: string): string {
    return formatTunis(iso, this.lang.current());
  }

  money(amount: number, currency: string): string {
    return formatMoney(amount, currency, this.lang.current());
  }

  another(): void {
    this.draft.created.set(null);
    this.draft.trip.set(null);
    this.draft.vehicleTypeId.set(null);
    this.draft.go(1);
  }
}
