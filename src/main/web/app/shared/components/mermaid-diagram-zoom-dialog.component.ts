import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideDownload } from '@ng-icons/lucide';
import { I18nService } from '../../core/i18n';
import { downloadSvgMarkup } from '../utils/download';
import { Z_MODAL_DATA } from './dialog';

export interface MermaidDiagramZoomData {
  svgMarkup: string;
  label?: string;
}

@Component({
  selector: 'app-mermaid-diagram-zoom-dialog',
  imports: [NgIcon],
  template: `
    <div class="flex flex-col gap-3 p-2">
      <div class="flex items-center justify-end">
        <button
          type="button"
          class="inline-flex size-6 cursor-pointer items-center justify-center rounded-md border border-border bg-background text-black transition-colors hover:bg-surface dark:text-white"
          [attr.aria-label]="i18n.t().chat.diagramDownload"
          (click)="download()"
        >
          <ng-icon name="lucideDownload" class="size-3 text-black dark:text-white" aria-hidden="true" />
        </button>
      </div>
      <div
        class="max-h-[80vh] w-full overflow-auto"
        role="img"
        [attr.aria-label]="data.label || i18n.t().chat.diagramLabel"
      >
        <div class="flex w-full justify-center p-2" [innerHTML]="safeSvg"></div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [provideIcons({ lucideDownload })],
})
export class MermaidDiagramZoomDialogComponent {
  private readonly sanitizer = inject(DomSanitizer);
  protected readonly i18n = inject(I18nService);
  readonly data = inject<MermaidDiagramZoomData>(Z_MODAL_DATA);

  readonly safeSvg = this.sanitizer.bypassSecurityTrustHtml(this.data.svgMarkup);

  download(): void {
    downloadSvgMarkup(this.data.svgMarkup, 'diagram.svg');
  }
}
