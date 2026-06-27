import { Component, ChangeDetectionStrategy, input } from '@angular/core';

@Component({
  selector: 'app-agent-panel',
  standalone: true,
  template: `
    <div class="flex flex-col gap-4">
      @if (!hideHeader()) {
        <div class="flex items-center justify-between p-4 bg-surface border rounded-xl">
          <div class="flex flex-col">
            <h3 class="text-lg font-semibold text-text m-0">{{ title() }}</h3>
            @if (description()) {
              <p class="text-xs text-[--color-text-secondary] m-0 mt-1">{{ description() }}</p>
            }
          </div>
          <div class="flex items-center">
            <ng-content select="[slot=headerRight]" />
          </div>
        </div>
      }
      <div class="bg-surface border rounded-xl p-6">
        <ng-content />
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgentPanelComponent {
  readonly title = input.required<string>();
  readonly description = input<string>('');
  readonly hideHeader = input<boolean>(false);
}
