import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';

export interface SegmentedControlOption<T extends string = string> {
  value: T;
  label: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-segmented-control',
  standalone: true,
  template: `
    <div class="container" role="tablist">
      @for (option of options(); track option.value) {
        <button
          type="button"
          class="option"
          [class.option--active]="value() === option.value"
          [class.option--disabled]="option.disabled"
          [attr.data-active]="value() === option.value ? 'true' : 'false'"
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
  styles: [`
    :host {
      display: inline-block;
    }

    .container {
      display: inline-flex;
      background: transparent;
      border-radius: 8px;
      padding: 3px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06), inset 0 1px 1px rgba(0, 0, 0, 0.06);
      gap: 2px;
    }

    .option {
      position: relative;
      padding: 8px 20px;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      font-size: 14px;
      font-weight: 500;
      color: #86868b;
      background: transparent;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.15s ease;
      white-space: nowrap;

      &:hover:not([data-active="true"]):not(:disabled) {
        color: #1d1d1f;
      }

      &:focus-visible {
        outline: none;
        box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.3);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      &--active {
        color: #1d1d1f;
        background: #ffffff;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
      }

      &--disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SegmentedControlComponent<T extends string = string> {
  options = input.required<SegmentedControlOption<T>[]>();
  value = input.required<T>();

  changed = output<T>();

  selectOption(option: SegmentedControlOption<T>): void {
    if (!option.disabled && option.value !== this.value()) {
      this.changed.emit(option.value);
    }
  }
}
