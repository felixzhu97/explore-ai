import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-chat-empty-state',
  template: `
    <div class="flex h-full flex-col items-center justify-center gap-4 text-center">
      <div class="text-4xl text-text-tertiary opacity-40">
        <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
      </div>
      <p class="text-lg font-medium text-text-secondary">{{ message() }}</p>
      <ng-content />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatEmptyStateComponent {
  readonly message = input.required<string>();
}
