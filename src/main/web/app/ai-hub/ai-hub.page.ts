import {
  Component,
  signal,
  computed,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SegmentedControlComponent } from '@shared/components/ui/segmented-control/segmented-control.component';
import { ChatTabComponent } from './chat/chat.component';
import { ImageComponent } from './image/image.component';
import { TtsPageComponent } from './tts/tts.component';
import { I18nService } from '@core/i18n';

type Tab = 'chat' | 'image' | 'tts';

@Component({
  selector: 'app-ai-hub',
  imports: [
    CommonModule,
    SegmentedControlComponent,
    ChatTabComponent,
    ImageComponent,
    TtsPageComponent,
  ],
  standalone: true,
  template: `
    <div class="flex flex-col gap-4">
      <div class="flex justify-center overflow-x-auto py-4">
        <app-segmented-control
          [options]="tabs()"
          [value]="activeTab()"
          (changed)="setTab($event)"
        />
      </div>

      <div class="animate-fade-in">
        @switch (activeTab()) {
          @case ('chat') {
            <app-chat-tab />
          }
          @case ('image') {
            <app-image-gen-tab (zoom)="onImageZoom($event)" />
          }
          @case ('tts') {
            <app-tts-tab />
          }
        }
      </div>

      @if (zoomedImage()) {
        <div
          class="fixed inset-0 z-1000 flex cursor-pointer items-center justify-center bg-black/90 backdrop-blur-md"
          (click)="closeZoom()"
          (keydown.escape)="closeZoom()"
          tabindex="-1"
          role="dialog"
          aria-modal="true"
        >
          <div class="relative max-h-[90vh] max-w-[90vw] animate-slide-in">
            <button
              type="button"
              class="absolute -top-10 -right-2 flex size-8 cursor-pointer items-center justify-center rounded-full bg-white/20 text-2xl text-white transition-colors hover:bg-white/30"
              (click)="closeZoom()"
              aria-label="Close"
            >
              ×
            </button>
            <img [src]="zoomedImage()" alt="Zoomed" class="max-h-[90vh] max-w-full object-contain" />
          </div>
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiHubPage {
  protected readonly i18n = inject(I18nService);

  readonly activeTab = signal<Tab>('chat');
  readonly zoomedImage = signal<string | null>(null);

  readonly tabs = computed(() => {
    const t = this.i18n.t().aiHub.tabs;
    return [
      { value: 'chat' as Tab, label: t.chat },
      { value: 'image' as Tab, label: t.image },
      { value: 'tts' as Tab, label: t.tts },
    ];
  });

  setTab(tab: Tab) {
    this.activeTab.set(tab);
  }

  onImageZoom(src: string) {
    this.zoomedImage.set(src);
  }

  closeZoom() {
    this.zoomedImage.set(null);
  }
}
