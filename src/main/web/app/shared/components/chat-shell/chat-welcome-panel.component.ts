import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { NxPrompt, NxPromptsComponent } from 'ng-zorro-x/prompts';
import { NxWelcomeComponent } from 'ng-zorro-x/welcome';

@Component({
  selector: 'app-chat-welcome-panel',
  imports: [NxWelcomeComponent, NxPromptsComponent],
  template: `
    <div
      class="
        mx-auto box-border flex size-full max-w-[880px] flex-col items-center
        justify-center gap-6 px-4 py-8
      "
    >
      <nx-welcome
        class="block w-auto max-w-full"
        variant="borderless"
        icon="✨"
        [title]="title()"
        [description]="description()"
      />
      @if (prompts().length > 0) {
        <section class="flex w-full flex-col items-center">
          <nx-prompts
            class="block w-auto max-w-full"
            [title]="promptsTitle()"
            [items]="prompts()"
            [wrap]="true"
            (itemClick)="onPromptClick($event)"
          />
        </section>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block h-full min-h-0 w-full' },
})
export class ChatWelcomePanelComponent {
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly promptsTitle = input('');
  readonly prompts = input<NxPrompt[]>([]);

  readonly promptSelect = output<string>();

  onPromptClick(prompt: NxPrompt): void {
    if (prompt.label) {
      this.promptSelect.emit(prompt.label);
    }
  }
}
