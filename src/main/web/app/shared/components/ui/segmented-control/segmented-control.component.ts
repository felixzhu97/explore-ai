import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import type { AbstractControl } from '@angular/forms';

export interface SegmentedControlOption<T extends string = string> {
  value: T;
  label: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-segmented-control',
  imports: [],
  standalone: true,
  template: `
    <div class="inline-flex gap-1 rounded-xl bg-surface p-1 shadow-card" role="tablist">
      @for (option of options(); track option.value) {
        <button
          type="button"
          class="
            relative rounded-lg px-5 py-2 text-sm font-medium transition-all
            duration-200
            focus-visible:ring-2 focus-visible:ring-primary/50
            focus-visible:outline-none
            disabled:cursor-not-allowed disabled:opacity-50
          "
          [class]="getOptionClasses(option)"
          [attr.role]="'tab'"
          [attr.aria-selected]="value() === option.value"
          [attr.aria-disabled]="option.disabled ? 'true' : 'false'"
          [disabled]="option.disabled"
          (click)="selectOption(option)"
        >
          {{ option.label }}
        </button>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SegmentedControlComponent<T extends string = string> {
  readonly options = input.required<SegmentedControlOption<T>[]>();
  readonly value = input.required<T>();
  readonly formControl = input<AbstractControl | null>(null);

  changed = output<T>();

  getOptionClasses(option: SegmentedControlOption<T>): string {
    const isActive = this.value() === option.value;
    const baseClasses = isActive
      ? 'text-text bg-surface-secondary shadow-sm'
      : 'text-text-tertiary hover:text-text';

    return `${baseClasses}`;
  }

  selectOption(option: SegmentedControlOption<T>): void {
    if (!option.disabled && option.value !== this.value()) {
      this.changed.emit(option.value);
    }
  }
}
