import { Component, inject, ChangeDetectionStrategy, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '@core/i18n';
import { ZardSegmentedComponent } from '@/shared/components/segmented';
import {
  MediaResultPanelComponent,
  MediaUploadPanelComponent,
} from '@shared/components/media-shell';
import { VisionService, type VisionTaskType } from './vision.service';

@Component({
  selector: 'app-vision-page',
  imports: [
    FormsModule,
    ZardSegmentedComponent,
    MediaUploadPanelComponent,
    MediaResultPanelComponent,
  ],
  templateUrl: './vision.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class VisionPageComponent {
  protected readonly vision = inject(VisionService);
  protected readonly i18n = inject(I18nService);

  readonly taskOptions = computed(() => {
    const t = this.i18n.t().imageUploader;
    return [
      { value: 'caption' as VisionTaskType, label: t.caption },
      { value: 'detect' as VisionTaskType, label: t.detect },
      { value: 'ocr' as VisionTaskType, label: t.ocr },
    ];
  });
}
