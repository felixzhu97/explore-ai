import { Component, ChangeDetectionStrategy, input } from '@angular/core';

export type CardVariant = 'default' | 'elevated' | 'outlined' | 'glass';
export type CardPadding = 'none' | 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-card',
  standalone: true,
  template: `
    <div
      class="block rounded-xl overflow-hidden transition-all duration-200"
      [class]="getClasses()"
    >
      <ng-content />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardComponent {
  readonly variant = input<CardVariant>('default');
  readonly padding = input<CardPadding>('md');
  readonly hoverable = input<boolean>(false);

  getClasses(): string {
    const classes: string[] = [];

    // Base classes
    classes.push('bg-surface');

    // Padding
    switch (this.padding()) {
      case 'none':
        break;
      case 'sm':
        classes.push('p-2');
        break;
      case 'lg':
        classes.push('p-6');
        break;
      default: // md
        classes.push('p-4');
    }

    // Variant
    switch (this.variant()) {
      case 'elevated':
        classes.push('shadow-elevated');
        break;
      case 'outlined':
        classes.push('border border-[#d1d1d6] shadow-none');
        break;
      case 'glass':
        classes.push('bg-glass backdrop-blur-xl border border-[--color-border-light] shadow-card');
        break;
      default: // default
        classes.push('shadow-card');
    }

    // Hoverable
    if (this.hoverable()) {
      classes.push('cursor-pointer hover:-translate-y-0.5 hover:shadow-card-hover active:translate-y-0');
    }

    return classes.join(' ');
  }
}
