import { Component, inject, computed, linkedSignal } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { I18nService } from '../i18n';
import { ZardSegmentedComponent } from '../ui/segmented';
import { MediaResultPanelComponent } from './media-result-panel.component';
import { MediaUploadPanelComponent } from './media-upload-panel.component';
import { VisionService, type VisionTaskType } from './vision.service';

const VISION_TASKS: readonly string[] = ['caption', 'detect', 'ocr'] satisfies VisionTaskType[];

function isVisionTask(value: string): value is VisionTaskType {
  return VISION_TASKS.includes(value);
}

@Component({
  selector: 'app-vision-page',
  imports: [
    FormField,
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

  readonly #selectedTask = linkedSignal(() => this.vision.activeTask());
  protected readonly taskField = form(this.#selectedTask);

  readonly taskOptions = computed(() => {
    const t = this.i18n.t().vision;
    return [
      { value: 'caption', label: t.caption },
      { value: 'detect', label: t.detect },
      { value: 'ocr', label: t.ocr },
    ];
  });

  onTaskChange(value: string): void {
    if (isVisionTask(value)) {
      this.vision.setActiveTask(value);
    }
  }
}
