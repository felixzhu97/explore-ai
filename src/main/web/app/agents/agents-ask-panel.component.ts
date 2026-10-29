import {
  Component,
  computed,
  inject,
  input,
  model,
  output,
} from '@angular/core';
import { I18nService } from '../i18n';
import { ZardButtonComponent } from '../ui/button';

export interface AgentsAskAgent {
  name: string;
  description: string;
}

export interface AgentsAskTurn {
  role: 'user' | 'assistant';
  text: string;
}

@Component({
  selector: 'app-agents-ask-panel',
  imports: [ZardButtonComponent],
  templateUrl: './agents-ask-panel.component.html',
  host: {
    class: 'flex min-h-96 min-w-0 flex-col lg:min-h-0',
  },
})
export class AgentsAskPanelComponent {
  protected readonly i18n = inject(I18nService);

  readonly agent = input<AgentsAskAgent | null>(null);
  readonly turns = input<AgentsAskTurn[]>([]);
  readonly isAsking = input(false);
  readonly suggestions = input<string[]>([]);
  readonly composer = model('');

  readonly ask = output<void>();
  readonly suggestionUsed = output<string>();

  protected readonly showEmpty = computed(
    () => this.turns().length === 0 && !this.isAsking(),
  );

  protected onComposerInput(event: Event): void {
    const target = event.target;
    if (target instanceof HTMLTextAreaElement) {
      this.composer.set(target.value);
    }
  }

  protected onComposerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.ask.emit();
    }
  }

  protected useSuggestion(text: string): void {
    this.suggestionUsed.emit(text);
  }
}
