import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { A2uiRendererService, SurfaceComponent } from '@a2ui/angular/v0_9';
import { MarkdownContentComponent } from '@shared/components/markdown-content.component';
import { splitMarkdownAndA2ui, type ContentSegment } from '@shared/utils/a2ui-fence';
import { EXPLORE_CHAT_CATALOG_ID } from '../../a2ui/catalog.constants';

@Component({
  selector: 'app-markdown-with-a2ui',
  imports: [MarkdownContentComponent, SurfaceComponent],
  template: `
    @for (segment of segments(); track trackSegment($index, segment)) {
      @switch (segment.type) {
        @case ('markdown') {
          <app-markdown-content [content]="segment.content" [streaming]="streaming()" />
        }
        @case ('a2ui') {
          @if (ingestError()) {
            <p class="my-2 text-sm text-text-secondary">{{ ingestError() }}</p>
          } @else {
            <div class="my-2 w-full overflow-hidden rounded-lg border border-border-light bg-surface-secondary/40 p-2">
              <a2ui-v09-surface [surfaceId]="segment.surfaceId" />
            </div>
          }
        }
        @case ('a2ui-pending') {
          <p class="my-2 text-sm text-text-tertiary">界面生成中…</p>
        }
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarkdownWithA2uiComponent {
  private readonly a2ui = inject(A2uiRendererService);

  readonly content = input.required<string>();
  readonly streaming = input(false);

  readonly segments = signal<ContentSegment[]>([]);
  readonly ingestError = signal<string | null>(null);

  private readonly processedFences = new Set<string>();

  constructor() {
    effect(() => {
      const next = splitMarkdownAndA2ui(this.content());
      this.segments.set(next);
      this.ingestError.set(null);

      for (const segment of next) {
        if (segment.type !== 'a2ui') {
          continue;
        }
        const key = segment.surfaceId;
        if (this.processedFences.has(key) || segment.messages.length === 0) {
          continue;
        }
        try {
          const withCatalog = segment.messages.map((message) => {
            if ('createSurface' in message && message.createSurface) {
              return {
                ...message,
                createSurface: {
                  ...message.createSurface,
                  catalogId: EXPLORE_CHAT_CATALOG_ID,
                },
              };
            }
            return message;
          });
          this.a2ui.processMessages(withCatalog);
          this.processedFences.add(key);
        } catch (error) {
          const message = error instanceof Error ? error.message : 'A2UI 渲染失败';
          this.ingestError.set(message);
        }
      }
    });
  }

  trackSegment(index: number, segment: ContentSegment): string {
    if (segment.type === 'a2ui') {
      return segment.surfaceId;
    }
    if (segment.type === 'a2ui-pending') {
      return `pending-${index}`;
    }
    return `md-${index}-${segment.content.length}`;
  }
}
