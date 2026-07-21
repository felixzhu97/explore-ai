import {
  ChangeDetectionStrategy,
  Component,
  input,
  model,
  output,
} from '@angular/core';
import { NxSenderComponent } from 'ng-zorro-x/sender';

@Component({
  selector: 'app-chat-sender-bar',
  imports: [NxSenderComponent],
  template: `
    <div
      class="shrink-0 border-t border-black/8 bg-white px-4 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]"
    >
      <ng-content select="[chatSenderPrelude]" />
      <div class="flex items-center gap-2">
        <ng-content select="[chatSenderActions]" />
        <nx-sender
          class="min-w-0 flex-1"
          [placeholder]="placeholder()"
          [(value)]="value"
          [loading]="loading()"
          (submitSend)="submitSend.emit()"
        />
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block shrink-0' },
})
export class ChatSenderBarComponent {
  readonly placeholder = input.required<string>();
  readonly loading = input(false);
  readonly value = model('');

  readonly submitSend = output<void>();
}
