import { Component, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-step-indicator',
  standalone: true,
  imports: [TranslateModule],
  template: `
    <ol class="flex items-center gap-2" [attr.aria-label]="'booking.title' | translate">
      @for (key of labels(); track key; let i = $index) {
        <li class="flex flex-1 items-center gap-2" [attr.aria-current]="current() === i + 1 ? 'step' : null">
          <span
            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-extrabold"
            [class.bg-brand]="current() >= i + 1"
            [class.text-white]="current() >= i + 1"
            [class.bg-white]="current() < i + 1"
            [class.text-muted]="current() < i + 1"
            [class.border]="current() < i + 1"
            [class.border-brand-line]="current() < i + 1"
          >{{ i + 1 }}</span>
          <span class="hidden text-sm font-semibold sm:inline" [class.text-muted]="current() !== i + 1">
            {{ key | translate }}
          </span>
          @if (i < labels().length - 1) {
            <span class="h-px flex-1 bg-brand-line" aria-hidden="true"></span>
          }
        </li>
      }
    </ol>
    <p class="mt-2 text-sm font-semibold sm:hidden">{{ labels()[current() - 1] | translate }}</p>
  `
})
export class StepIndicatorComponent {
  readonly labels = input.required<string[]>();
  readonly current = input.required<number>();
}
