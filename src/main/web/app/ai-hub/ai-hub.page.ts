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
    <div class="ai-hub">
      <div class="tab-header">
        <app-segmented-control
          [options]="tabs()"
          [value]="activeTab()"
          (changed)="setTab($event)"
        />
      </div>

      <div class="tab-content">
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
          class="zoom-modal"
          (click)="closeZoom()"
          (keydown.escape)="closeZoom()"
          tabindex="-1"
          role="dialog"
          aria-modal="true"
        >
          <div class="zoom-content">
            <button
              type="button"
              class="zoom-close"
              (click)="closeZoom()"
              aria-label="Close"
            >
              ×
            </button>
            <img [src]="zoomedImage()" alt="Zoomed" />
          </div>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .ai-hub {
        display: flex;
        flex-direction: column;
        gap: 16px;
      }

      .tab-header {
        display: flex;
        justify-content: center;
        padding: 16px 0;
        overflow-x: auto;
        -webkit-overflow-scrolling: touch;
        scrollbar-width: none;
        -ms-overflow-style: none;
      }

      .tab-header::-webkit-scrollbar {
        display: none;
      }

      .tab-content {
        animation: fadeIn 0.3s ease;
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
          transform: translateY(8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      /* Zoom Modal */
      .zoom-modal {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.9);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
        cursor: pointer;
      }

      .zoom-content {
        position: relative;
        max-width: 90vw;
        max-height: 90vh;
      }

      .zoom-content img {
        max-width: 100%;
        max-height: 90vh;
        object-fit: contain;
      }

      .zoom-close {
        position: absolute;
        top: -40px;
        right: 0;
        width: 32px;
        height: 32px;
        background: rgba(255, 255, 255, 0.2);
        border: none;
        border-radius: 50%;
        color: white;
        font-size: 24px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .zoom-close:hover {
        background: rgba(255, 255, 255, 0.3);
      }
    `,
  ],
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
