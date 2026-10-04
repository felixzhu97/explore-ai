import { Component, input } from '@angular/core';
import type { ToolStep } from './chat-bubble-list.component';

@Component({
  selector: 'app-chat-tool-steps',
  template: `
    @for (step of steps(); track step.name + step.label + $index) {
      <div class="text-xs text-text-secondary">
        @switch (step.status) {
          @case ('running') {
            <span>{{ step.label }}</span>
          }
          @case ('success') {
            <span>{{ step.label }} · {{ doneLabel() }}</span>
          }
          @default {
            <span>{{ step.label }} · {{ failedLabel() }}</span>
          }
        }
      </div>
    }
  `,
  host: {
    class: 'mb-2 flex flex-col gap-1',
    '[hidden]': 'steps().length === 0',
  },
})
export class ChatToolStepsComponent {
  readonly steps = input<readonly ToolStep[]>([]);
  readonly doneLabel = input('Done');
  readonly failedLabel = input('Failed');
}
