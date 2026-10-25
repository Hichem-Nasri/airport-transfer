import { Component, computed, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { BookingStatus } from '../core/booking.service';

const BADGES: Record<BookingStatus, string> = {
  PENDING: 'bg-brand-soft text-brand-dark',
  CONFIRMED: 'bg-ink text-white',
  ASSIGNED: 'bg-ink text-white',
  IN_PROGRESS: 'bg-ink text-white',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-gray-200 text-muted'
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [TranslateModule],
  template: `<span class="inline-block rounded-full px-3 py-1 text-sm font-semibold" [class]="classes()">{{ 'status.' + status() | translate }}</span>`
})
export class StatusBadgeComponent {
  readonly status = input.required<BookingStatus>();
  readonly classes = computed(() => BADGES[this.status()]);
}
