import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-button',
  standalone: true,
  template: `
    <button
      type="button"
      class="
        inline-flex items-center justify-center leading-none font-medium
        whitespace-nowrap transition-all duration-200 select-none
        focus-visible:ring-2 focus-visible:ring-primary/50
        focus-visible:outline-none
        disabled:cursor-not-allowed disabled:opacity-50
      "
      [class]="getClasses()"
      [disabled]="disabled() || loading()"
      [attr.aria-busy]="loading() ? true : null"
      (click)="handleClick($event)"
    >
      @if (loading()) {
        <span class="
          inline-block size-3.5 animate-spin rounded-full border-2
          border-current border-r-transparent
        " aria-hidden="true"></span>
      } @else if (icon()) {
        <span class="inline-flex items-center">
          <ng-content select="[icon]" />
        </span>
        <ng-content />
      } @else {
        <ng-content />
      }
    </button>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ButtonComponent {
  readonly variant = input<ButtonVariant>('primary');
  readonly size = input<ButtonSize>('md');
  readonly loading = input<boolean>(false);
  readonly fullWidth = input<boolean>(false);
  readonly disabled = input<boolean>(false);
  readonly icon = input<boolean>(false);

  clicked = output<MouseEvent>();

  getClasses(): string {
    const classes: string[] = [];

    // Size classes
    switch (this.size()) {
      case 'sm':
        classes.push('px-3 py-1.5 text-xs rounded-sm gap-1');
        break;
      case 'lg':
        classes.push('px-6 py-3.5 text-base rounded-[10px] gap-2');
        break;
      default: // md
        classes.push('px-[18px] py-2.5 text-sm rounded-md gap-1.5');
    }

    // Variant classes
    switch (this.variant()) {
      case 'secondary':
        classes.push('bg-surface text-primary border border-[#d1d1d6] shadow-sm hover:bg-primary-light hover:border-primary active:bg-primary-light active:scale-[0.98]');
        break;
      case 'ghost':
        classes.push('bg-transparent text-primary hover:bg-primary-light active:bg-primary-light/12 active:scale-[0.98]');
        break;
      case 'danger':
        classes.push('bg-error text-white shadow-sm hover:bg-[#e6352b] hover:shadow-[0_4px_12px_rgba(0,0,0,0.15)] active:bg-[#cc3126] active:scale-[0.98]');
        break;
      default: // primary
        classes.push('bg-primary text-white shadow-sm hover:bg-primary-hover hover:shadow-[0_4px_12px_rgba(0,0,0,0.15)] active:bg-primary-active active:scale-[0.98]');
    }

    // Full width
    if (this.fullWidth()) {
      classes.push('w-full');
    }

    return classes.join(' ');
  }

  handleClick(event: MouseEvent): void {
    if (!this.disabled() && !this.loading()) {
      this.clicked.emit(event);
    }
  }
}
