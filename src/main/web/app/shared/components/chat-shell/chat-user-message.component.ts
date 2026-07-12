import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-chat-user-message',
  template: `
    <div class="flex flex-row items-end justify-end gap-2">
      <div class="rounded-2xl bg-primary px-4 py-3 text-base leading-relaxed wrap-break-word text-white">
        {{ content() }}
        @if (images()?.length) {
          <div class="mt-2 flex flex-wrap gap-2">
            @for (img of images(); track $index) {
              <img [src]="img" alt="Uploaded image" class="max-h-48 max-w-48 rounded-lg object-contain" />
            }
          </div>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatUserMessageComponent {
  readonly content = input.required<string>();
  readonly images = input<string[]>();
}
