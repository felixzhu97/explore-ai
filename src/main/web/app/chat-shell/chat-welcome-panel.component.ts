import {
  Component,
  input,
  output,
} from '@angular/core';
import { type NxPrompt, NxPromptsComponent } from 'ng-zorro-x/prompts';
import { NxWelcomeComponent } from 'ng-zorro-x/welcome';
import { hasText } from '../shared/presence';

@Component({
  selector: 'app-chat-welcome-panel',
  imports: [NxWelcomeComponent, NxPromptsComponent],
  template: `
    <div
      class="
        mx-auto box-border flex size-full max-w-220 flex-col items-center
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
  host: { class: 'block h-full min-h-0 w-full' },
})
export class ChatWelcomePanelComponent {
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly promptsTitle = input('');
  readonly prompts = input<NxPrompt[]>([]);

  readonly promptSelected = output<string>();

  onPromptClick(prompt: NxPrompt): void {
    if (hasText(prompt.label)) {
      this.promptSelected.emit(prompt.label);
    }
  }
}
