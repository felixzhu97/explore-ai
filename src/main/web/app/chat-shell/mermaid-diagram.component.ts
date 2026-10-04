import {
  Component,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { DomSanitizer, type SafeHtml } from '@angular/platform-browser';
import DOMPurify from 'dompurify';
import { I18nService } from '../i18n';
import { ZardDialogService } from '../ui/dialog';
import { MermaidDiagramZoomDialogComponent } from './mermaid-diagram-zoom-dialog.component';

interface MermaidApi {
  initialize: (config: Record<string, unknown>) => void;
  render: (
    id: string,
    text: string,
  ) => Promise<{ svg: string; bindFunctions?: (element: Element) => void }>;
}

let mermaidInitPromise: Promise<MermaidApi> | null = null;
let renderSeq = 0;

async function loadMermaid(): Promise<MermaidApi> {
  mermaidInitPromise ??= import('mermaid').then((mod) => {
    const api = mod.default as MermaidApi;
    api.initialize({
      startOnLoad: false,
      securityLevel: 'strict',
      flowchart: { htmlLabels: false },
    });
    return api;
  });
  return mermaidInitPromise;
}

@Component({
  selector: 'app-mermaid-diagram',
  template: `
    @if (safeSvg(); as svg) {
      <button
        type="button"
        class="my-2 flex w-full cursor-zoom-in items-center justify-center overflow-x-auto rounded-lg outline-none focus-visible:ring-2 focus-visible:ring-ring"
        [attr.aria-label]="i18n.t().chat.diagramExpand"
        (click)="openFullscreen()"
      >
        <div
          class="flex w-full items-center justify-center"
          role="img"
          [attr.aria-label]="i18n.t().chat.diagramLabel"
          [innerHTML]="svg"
        ></div>
      </button>
    } @else if (hasError()) {
      <div class="my-2">
        <p class="mb-1 text-sm text-text-secondary">{{ i18n.t().chat.diagramRenderFailed }}</p>
        <pre class="overflow-x-auto rounded-lg bg-surface p-3 text-xs"><code>{{ source() }}</code></pre>
      </div>
    } @else {
      <p class="my-2 text-sm text-text-tertiary">{{ i18n.t().chat.diagramRendering }}</p>
    }
  `,
})
export class MermaidDiagramComponent {
  readonly #sanitizer = inject(DomSanitizer);
  readonly #dialog = inject(ZardDialogService);
  protected readonly i18n = inject(I18nService);

  readonly source = input.required<string>();
  readonly diagramId = input.required<string>();

  readonly safeSvg = signal<SafeHtml | null>(null);
  readonly svgMarkup = signal('');
  readonly hasError = signal(false);

  constructor() {
    effect(() => {
      const source = this.source();
      const diagramId = this.diagramId();
      untracked(() => {
        this.safeSvg.set(null);
        this.svgMarkup.set('');
        this.hasError.set(false);
      });
      void this.#renderDiagram(source, diagramId);
    });
  }

  openFullscreen(): void {
    const markup = this.svgMarkup();
    if (!markup) {
      return;
    }
    this.#dialog.create({
      zContent: MermaidDiagramZoomDialogComponent,
      zData: {
        svgMarkup: markup,
        label: this.i18n.t().chat.diagramLabel,
      },
      zHideFooter: true,
      zWidth: '95vw',
      zMaskClosable: true,
      zCustomClasses: 'border-none bg-background/95 shadow-2xl',
    });
  }

  async #renderDiagram(source: string, diagramId: string): Promise<void> {
    const trimmed = source.trim();
    if (!trimmed) {
      this.hasError.set(true);
      return;
    }

    try {
      const mermaid = await loadMermaid();
      // Unique DOM id per render — Mermaid requires unique ids in the document.
      const renderId = `${diagramId}-${++renderSeq}`;
      const { svg } = await mermaid.render(renderId, trimmed);
      const clean = DOMPurify.sanitize(svg, {
        USE_PROFILES: { svg: true, svgFilters: true },
        ADD_TAGS: ['foreignObject'],
      });
      if (!clean) {
        this.hasError.set(true);
        return;
      }
      this.svgMarkup.set(clean);
      this.safeSvg.set(this.#sanitizer.bypassSecurityTrustHtml(clean));
      this.hasError.set(false);
    } catch {
      this.safeSvg.set(null);
      this.svgMarkup.set('');
      this.hasError.set(true);
    }
  }
}
