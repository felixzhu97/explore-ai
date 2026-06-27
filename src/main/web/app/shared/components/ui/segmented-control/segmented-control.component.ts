import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';

export interface SegmentedControlOption<T extends string = string> {
  value: T;
  label: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-segmented-control',
  standalone: true,
  template: `
    <div class="inline-flex bg-surface rounded-xl shadow-card p-1 gap-1" role="tablist">
      @for (option of options(); track option.value) {
        <button
          type="button"
          class="relative px-5 py-2 text-sm font-medium rounded-lg transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50 disabled:opacity-50 disabled:cursor-not-allowed"
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
