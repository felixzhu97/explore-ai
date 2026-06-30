import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface FeatureCardData {
  id: 'chat' | 'image' | 'tts';
  title: string;
  description: string;
  icon: 'chat' | 'image' | 'audio';
  route: string;
}

@Component({
  selector: 'app-feature-card',
  imports: [RouterLink],
  standalone: true,
  template: `
    <a
      [routerLink]="data().route"
      class="group flex flex-col gap-4 rounded-xl bg-surface p-6 shadow-card transition-all duration-200 hover:-translate-y-1 hover:shadow-card-hover focus-visible:ring-2 focus-visible:ring-primary focus-visible:outline-none"
      [tabindex]="0"
      (keydown.enter)="onKeydown()"
      (keydown.space)="onKeydown(); $event.preventDefault()"
    >
      <div class="flex size-12 items-center justify-center rounded-xl bg-primary-light text-primary transition-transform duration-200 group-hover:scale-110">
        <svg
          class="size-6"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          @switch (data().icon) {
            @case ('chat') {
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              <path d="M8 10h.01" />
              <path d="M12 10h.01" />
              <path d="M16 10h.01" />
            }
            @case ('image') {
              <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
              <circle cx="9" cy="9" r="2" />
              <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21" />
            }
            @case ('audio') {
              <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z" />
              <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
              <line x1="12" x2="12" y1="19" y2="22" />
            }
          }
        </svg>
      </div>

      <div class="flex flex-col gap-2">
        <h3 class="text-lg font-semibold text-text">
          {{ data().title }}
        </h3>
        <p class="text-sm leading-relaxed text-text-secondary">
          {{ data().description }}
        </p>
      </div>

      <div class="mt-auto flex items-center gap-1 text-sm font-medium text-primary opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus-visible:opacity-100">
        <span>Explore</span>
        <svg
          class="size-4 transition-transform duration-200 group-hover:translate-x-1"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M5 12h14" />
          <path d="m12 5 7 7-7 7" />
        </svg>
      </div>
    </a>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeatureCardComponent {
  readonly data = input.required<FeatureCardData>();
  readonly selected = output<FeatureCardData['id']>();

  onKeydown(): void {
    this.selected.emit(this.data().id);
  }
}
