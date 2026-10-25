import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslateModule } from '@ngx-translate/core';
import { Airport, CatalogService, ZoneWithAreas } from '../../core/catalog.service';
import { LanguageService } from '../../core/language.service';
import { localName } from '../../core/localized';
import { formatTunis } from '../../core/tunis-time';
import { BookingDraftService } from './booking-draft.service';

/** Recap of the trip chosen in step 1, shown above steps 2 and 3. */
@Component({
  selector: 'app-route-summary',
  standalone: true,
  imports: [TranslateModule],
  template: `
    @if (view(); as v) {
      <section class="rounded-2xl border border-brand-line bg-white p-4" [attr.aria-label]="'booking.summary.title' | translate">
        <p class="flex flex-wrap items-center gap-x-2 font-extrabold">
          <span>{{ v.from }}</span>
          <span aria-hidden="true" class="text-brand rtl:rotate-180">→</span>
          <span>{{ v.to }}</span>
        </p>
        <p class="mt-1 text-sm text-muted">
          {{ v.when }} · {{ 'booking.passengersCount' | translate: { count: v.passengers } }}
        </p>
        @if (v.back) {
          <p class="mt-1 text-sm text-muted">{{ 'booking.returnOn' | translate }} {{ v.back }}</p>
        }
      </section>
    }
  `
})
export class RouteSummaryComponent {
  private readonly draft = inject(BookingDraftService);
  private readonly lang = inject(LanguageService);
  private readonly catalog = inject(CatalogService);
  private readonly airports = toSignal(this.catalog.airports(), { initialValue: [] as Airport[] });
  private readonly zones = toSignal(this.catalog.zones(), { initialValue: [] as ZoneWithAreas[] });

  readonly view = computed(() => {
    const trip = this.draft.trip();
    if (!trip) {
      return null;
    }
    const lang = this.lang.current();
    const airport = this.airports().find((a) => a.id === trip.airportId);
    const area = this.zones().flatMap((z) => z.areas).find((a) => a.id === trip.areaId);
    const airportLabel = airport ? `${airport.iataCode} · ${localName(airport, lang)}` : '';
    const areaLabel = area ? localName(area, lang) : '';
    const toAirport = trip.direction === 'ADDRESS_TO_AIRPORT';
    return {
      from: toAirport ? areaLabel : airportLabel,
      to: toAirport ? airportLabel : areaLabel,
      when: formatTunis(trip.pickupAt, lang),
      back: trip.returnPickupAt ? formatTunis(trip.returnPickupAt, lang) : null,
      passengers: trip.passengers
    };
  });
}
