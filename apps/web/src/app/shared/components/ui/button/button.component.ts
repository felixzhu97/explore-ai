import { Component, ChangeDetectionStrategy, input, output, computed } from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-button',
  standalone: true,
  template: `
    <button
      [class]="
        'button button--' +
        variant() +
        ' button--' +
        size() +
        (fullWidth() ? ' button--full-width' : '')
      "
      [disabled]="disabled() || loading()"
      [attr.aria-busy]="loading() ? true : null"
      (click)="handleClick($event)"
    >
      @if (loading()) {
        <span class="spinner" aria-hidden="true"></span>
      } @else if (icon()) {
        <span class="icon-wrapper">
          <ng-content select="[icon]"></ng-content>
        </span>
        <ng-content></ng-content>
      } @else {
        <ng-content></ng-content>
      }
    </button>
  `,
  styles: [
    `
      :host {
        display: inline-block;
      }

      .button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        font-weight: 500;
        line-height: 1;
        border: none;
        cursor: pointer;
        transition: all 0.2s ease;
        white-space: nowrap;
        user-select: none;

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        &:focus-visible {
          outline: none;
          box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.3);
        }

        // Size variants
        &--sm {
          padding: 6px 12px;
          font-size: 12px;
          border-radius: 6px;
          gap: 4px;
        }

        &--md {
          padding: 10px 18px;
          font-size: 14px;
          border-radius: 8px;
          gap: 6px;
        }

        &--lg {
          padding: 14px 24px;
          font-size: 15px;
          border-radius: 10px;
          gap: 8px;
        }

        // Full width
        &--full-width {
          width: 100%;
        }

        // Color variants
        &--primary {
          background: #007aff;
          color: white;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

          &:hover:not(:disabled) {
            background: #0066d6;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
          }

          &:active:not(:disabled) {
            background: #0055b3;
            transform: scale(0.98);
          }
        }

        &--secondary {
          background: #ffffff;
          color: #007aff;
          border: 1px solid #d1d1d6;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

          &:hover:not(:disabled) {
            background: rgba(0, 122, 255, 0.08);
            border-color: #007aff;
          }

          &:active:not(:disabled) {
            background: rgba(0, 122, 255, 0.12);
            transform: scale(0.98);
          }
        }

        &--ghost {
          background: transparent;
          color: #007aff;

          &:hover:not(:disabled) {
            background: rgba(0, 122, 255, 0.08);
          }

          &:active:not(:disabled) {
            background: rgba(0, 122, 255, 0.12);
            transform: scale(0.98);
          }
        }

        &--danger {
          background: #ff3b30;
          color: white;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

          &:hover:not(:disabled) {
            background: #e6352b;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
          }

          &:active:not(:disabled) {
            background: #cc3126;
            transform: scale(0.98);
          }
        }
      }

      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }

      .spinner {
        display: inline-block;
        width: 14px;
        height: 14px;
        border: 2px solid currentColor;
        border-right-color: transparent;
        border-radius: 50%;
        animation: spin 0.6s linear infinite;
      }

      .icon-wrapper {
        display: inline-flex;
        align-items: center;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ButtonComponent {
  variant = input<ButtonVariant>('primary');
  size = input<ButtonSize>('md');
  loading = input<boolean>(false);
  fullWidth = input<boolean>(false);
  disabled = input<boolean>(false);
  icon = input<boolean>(false);

  clicked = output<MouseEvent>();

  handleClick(event: MouseEvent): void {
    if (!this.disabled() && !this.loading()) {
      this.clicked.emit(event);
    }
  }
}
