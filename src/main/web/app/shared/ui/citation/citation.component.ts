import { Component, ChangeDetectionStrategy, input } from '@angular/core';

@Component({
  selector: 'app-citation',
  template: `
    @if (url()) {
      <a
        [href]="url()"
        target="_blank"
        rel="noopener noreferrer"
        class="
          inline-flex items-center gap-1.5 text-xs text-primary/70
          transition-colors duration-150 hover:text-primary hover:underline
          focus-visible:rounded-sm focus-visible:ring-2 focus-visible:ring-primary/50 focus-visible:outline-none
        "
        [attr.aria-label]="ariaLabel()"
      >
        <svg
          class="size-3.5 flex-shrink-0"
          viewBox="0 0 16 16"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <path
            d="M6.5 3.5H3.5C2.67157 3.5 2 4.17157 2 5V13C2 13.8284 2.67157 14.5 3.5 14.5H11.5C12.3284 14.5 13 13.8284 13 13V10M9.5 2H14M14 2V6.5M14 2L7 9"
            stroke="currentColor"
            stroke-width="1.25"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <span class="max-w-[200px] truncate">{{ displayTitle() }}</span>
      </a>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitationComponent {
  readonly url = input.required<string>();
  readonly title = input<string>();

  displayTitle(): string {
    return this.title() || this.truncateUrl();
  }

  ariaLabel(): string {
    const title = this.title();
    return title
      ? `Open citation: ${title} (opens in new tab)`
      : `Open citation: ${this.url()} (opens in new tab)`;
  }

  private truncateUrl(): string {
    const url = this.url();
    if (url.length <= 40) {
      return url;
    }
    return url.substring(0, 40) + '...';
  }
}
