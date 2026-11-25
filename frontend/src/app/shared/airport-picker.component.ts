import { Component, computed, inject, input, model } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Airport } from '../core/catalog.service';
import { LanguageService } from '../core/language.service';
import { localName } from '../core/localized';

@Component({
  selector: 'app-airport-picker',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  template: `
    <label class="field-label" [for]="inputId()">{{ label() | translate }}</label>
    <select [id]="inputId()" class="field-input" [name]="inputId()" [attr.aria-invalid]="invalid()"
            [ngModel]="airportId()" (ngModelChange)="choose($any($event))">
      <option [ngValue]="null">{{ 'booking.choose' | translate }}</option>
      @for (g of groups(); track g.country) {
        <optgroup [label]="'booking.country.' + g.country | translate">
          @for (a of g.airports; track a.id) {
            <option [ngValue]="a.id">{{ a.iataCode }} — {{ name(a) }}</option>
          }
        </optgroup>
      }
    </select>
    @if (terminals().length > 0) {
      <label class="field-label mt-3" [for]="inputId() + '-terminal'">{{ 'booking.fields.terminal' | translate }}</label>
      <select [id]="inputId() + '-terminal'" class="field-input" [name]="inputId() + '-terminal'"
              [ngModel]="terminal()" (ngModelChange)="terminal.set($any($event))">
        <option value="">{{ 'booking.terminalAny' | translate }}</option>
        @for (t of terminals(); track t) {
          <option [value]="t">{{ t }}</option>
        }
      </select>
    }
  `
})
export class AirportPickerComponent {
  private readonly lang = inject(LanguageService);

  readonly airports = input.required<Airport[]>();
  readonly inputId = input.required<string>();
  readonly label = input('booking.fields.airport');
  readonly invalid = input(false);
  readonly airportId = model<number | null>(null);
  readonly terminal = model('');

  readonly groups = computed(() => {
    const byCountry = new Map<string, Airport[]>();
    for (const a of this.airports()) {
      byCountry.set(a.country, [...(byCountry.get(a.country) ?? []), a]);
    }
    return [...byCountry.entries()]
      .sort(([a], [b]) => (a === 'FR' ? -1 : b === 'FR' ? 1 : a.localeCompare(b)))
      .map(([country, airports]) => ({ country, airports }));
  });

  readonly terminals = computed(() => {
    const list = this.airports().find((a) => a.id === this.airportId())?.terminals;
    return list ? list.split(',').map((t) => t.trim()).filter((t) => t.length > 0) : [];
  });

  choose(id: number | null): void {
    this.airportId.set(id);
    this.terminal.set('');
  }

  name(item: Airport): string {
    return localName(item, this.lang.current());
  }
}
