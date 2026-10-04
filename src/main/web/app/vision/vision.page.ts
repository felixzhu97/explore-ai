import { Component, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../i18n';
import { ZardSegmentedComponent } from '../ui/segmented';
import { MediaResultPanelComponent } from './media-result-panel.component';
import { MediaUploadPanelComponent } from './media-upload-panel.component';
import { VisionService } from './vision.service';

@Component({
  selector: 'app-vision-page',
  imports: [
    FormsModule,
    ZardSegmentedComponent,
    MediaUploadPanelComponent,
    MediaResultPanelComponent,
  ],
  templateUrl: './vision.page.html',
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class VisionPageComponent {
  protected readonly vision = inject(VisionService);
  protected readonly i18n = inject(I18nService);

  readonly taskOptions = computed(() => {
    const t = this.i18n.t().vision;
    return [
      { value: 'caption', label: t.caption },
      { value: 'detect', label: t.detect },
      { value: 'ocr', label: t.ocr },
    ];
  });
}
