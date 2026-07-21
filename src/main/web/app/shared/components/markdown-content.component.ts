import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { MarkdownService } from '../utils/markdown.service';

@Component({
  selector: 'app-markdown-content',
  template: `<div [innerHTML]="html()"></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class:
      '[&_blockquote]:border-l-2 [&_blockquote]:border-primary [&_blockquote]:pl-3 [&_blockquote]:text-text-secondary [&_blockquote]:italic [&_code]:rounded [&_code]:bg-surface [&_code]:px-1 [&_code]:py-0.5 [&_code]:font-mono [&_code]:text-sm [&_h1]:mb-3 [&_h1]:text-xl [&_h1]:font-semibold [&_h2]:mb-2 [&_h2]:text-lg [&_h2]:font-semibold [&_h3]:mb-2 [&_h3]:text-base [&_h3]:font-semibold [&_li]:my-0.5 [&_ol]:my-2 [&_ol]:list-decimal [&_ol]:pl-5 [&_p]:mb-2 [&_p]:last:mb-0 [&_pre]:my-2 [&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:bg-surface [&_pre]:p-3 [&_pre]:text-sm [&_ul]:my-2 [&_ul]:list-disc [&_ul]:pl-5',
  },
})
export class MarkdownContentComponent {
  private readonly markdown = inject(MarkdownService);

  readonly content = input.required<string>();
  readonly streaming = input(false);

  readonly html = computed(() => this.markdown.render(this.content(), this.streaming()),
  );
}
