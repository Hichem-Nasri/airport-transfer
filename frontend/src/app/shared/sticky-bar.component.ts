import { Component, input, output } from '@angular/core';

/** Fixed bottom bar of the booking flow: the total on the left, the primary action on the right. */
@Component({
  selector: 'app-sticky-bar',
  standalone: true,
  template: `
    <div class="fixed inset-x-0 bottom-0 z-20 border-t border-brand-line bg-white px-4 py-3 shadow-[0_-4px_16px_rgba(11,27,51,0.06)]">
      <div class="mx-auto flex max-w-2xl items-center gap-4">
        <div class="min-w-0 flex-1">
          @if (totalLabel()) {
            <p class="text-xs font-semibold text-muted">{{ totalLabel() }}</p>
            <p class="truncate text-lg font-extrabold">{{ total() }}</p>
          }
        </div>
        <button type="button" class="btn-primary shrink-0 px-6" [disabled]="disabled()" (click)="action.emit()">
          {{ label() }}
        </button>
      </div>
    </div>
  `
})
export class StickyBarComponent {
  readonly label = input.required<string>();
  readonly total = input<string>('');
  readonly totalLabel = input<string>('');
  readonly disabled = input(false);
  readonly action = output<void>();
}
