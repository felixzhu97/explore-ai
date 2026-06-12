import { Component, ChangeDetectionStrategy, input, signal } from '@angular/core';

export type CardVariant = 'default' | 'elevated' | 'outlined' | 'glass';
export type CardPadding = 'none' | 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-card',
  standalone: true,
  template: `
    <div
      [class]="
        'card card--' +
        variant() +
        ' card--padding-' +
        padding() +
        (hoverable() ? ' card--interactive' : '')
      "
      (click)="handleClick()"
      (mouseenter)="isHovered.set(true)"
      (mouseleave)="isHovered.set(false)"
    >
      <ng-content></ng-content>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .card {
        border-radius: 12px;
        overflow: hidden;
        transition: all 0.2s ease;
        padding: 16px;
        background: #ffffff;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

        &--elevated {
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
        }

        &--outlined {
          background: #ffffff;
          border: 1px solid #d1d1d6;
          box-shadow: none;
        }

        &--glass {
          background: rgba(255, 255, 255, 0.8);
          backdrop-filter: blur(20px) saturate(180%);
          -webkit-backdrop-filter: blur(20px) saturate(180%);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
          border: 1px solid rgba(0, 0, 0, 0.06);
        }

        &--padding-none {
          padding: 0;
        }
        &--padding-sm {
          padding: 8px;
        }
        &--padding-md {
          padding: 16px;
        }
        &--padding-lg {
          padding: 24px;
        }

        &--interactive {
          cursor: pointer;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
          }

          &:active {
            transform: translateY(0);
          }
        }
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardComponent {
  variant = input<CardVariant>('default');
  padding = input<CardPadding>('md');
  hoverable = input<boolean>(false);

  protected isHovered = signal(false);

  handleClick(): void {
    // Card click handling if needed
  }
}
