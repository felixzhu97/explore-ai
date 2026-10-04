import {
  Component,
  computed,
  inject,
  input,
  ViewEncapsulation,
} from '@angular/core';
import { MarkdownService } from './markdown.service';

@Component({
  selector: 'app-markdown-content',
  template: `<div [innerHTML]="html()"></div>`,
  styleUrl: './markdown-content.component.css',
  encapsulation: ViewEncapsulation.None,
  host: { class: 'markdown-content' },
})
export class MarkdownContentComponent {
  readonly #markdown = inject(MarkdownService);

  readonly content = input.required<string>();
  readonly isStreaming = input(false);

  readonly html = computed(() => {
    return this.#markdown.render(this.content(), this.isStreaming());
  });
}
