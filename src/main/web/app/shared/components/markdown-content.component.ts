import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  ViewEncapsulation,
} from '@angular/core';
import { MarkdownService } from '../utils/markdown.service';

@Component({
  selector: 'app-markdown-content',
  template: `<div [innerHTML]="html()"></div>`,
  styleUrl: './markdown-content.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  host: { class: 'markdown-content' },
})
export class MarkdownContentComponent {
  private readonly markdown = inject(MarkdownService);

  readonly content = input.required<string>();
  readonly streaming = input(false);

  readonly html = computed(() => this.markdown.render(this.content(), this.streaming()));
}
