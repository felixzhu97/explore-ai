import { Component, ChangeDetectionStrategy, input } from '@angular/core';

@Component({
  selector: 'app-agent-panel',
  standalone: true,
  template: `
    <div class="flex flex-col gap-4">
      @if (!hideHeader()) {
        <div class="
          flex items-center justify-between rounded-xl bg-surface p-4
        ">
          <div class="flex flex-col">
            <h3 class="m-0 text-lg font-semibold text-text">{{ title() }}</h3>
            @if (description()) {
              <p class="m-0 mt-1 text-xs text-text-secondary">{{ description() }}</p>
            }
          </div>
          <div class="flex items-center">
            <ng-content select="[slot=headerRight]" />
          </div>
        </div>
      }
      <div class="rounded-xl bg-surface p-6">
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
