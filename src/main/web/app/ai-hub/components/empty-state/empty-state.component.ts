import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <div class="flex flex-col items-center justify-center gap-6 py-12 text-center">
      <div class="relative">
        <svg
          class="size-24 text-text-tertiary"
          viewBox="0 0 100 100"
          fill="none"
        >
          <circle cx="50" cy="50" r="40" stroke="currentColor" stroke-width="2" opacity="0.2" />
          <circle cx="50" cy="50" r="30" stroke="currentColor" stroke-width="2" opacity="0.3" />
          <circle cx="50" cy="50" r="20" stroke="currentColor" stroke-width="2" opacity="0.4" />
          <circle cx="50" cy="50" r="10" fill="currentColor" opacity="0.2" />
        </svg>
      </div>

      <div class="flex flex-col gap-2">
        <h3 class="text-lg font-semibold text-text">
          {{ title() }}
        </h3>
        <p class="max-w-sm text-sm text-text-secondary">
          {{ message() }}
        </p>
      </div>

      @if (showCta()) {
        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-primary-hover focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:outline-none"
          (click)="onCtaClick()"
        >
          @if (ctaLabel()) {
            {{ ctaLabel() }}
          } @else {
            Get Started
          }
        </button>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  readonly title = input<string>('');
  readonly message = input<string>('');
  readonly showCta = input<boolean>(false);
  readonly ctaLabel = input<string>('');

  readonly ctaClick = output<void>();

  onCtaClick(): void {
    this.ctaClick.emit();
  }
}
