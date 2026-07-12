import { Component, signal, inject, ChangeDetectionStrategy, computed } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiMediaService } from '@core/services/api-media.service';
import { I18nService } from '@core/i18n';
import { SegmentedControlComponent } from '@shared/components/ui/segmented-control/segmented-control.component';
import { DetectionOverlayComponent } from './detection-overlay.component';
import type { VisionResult } from './vision.model';

type TaskType = 'caption' | 'detect' | 'ocr';

interface TabState {
  image: string | null;
  file: File | null;
  result: VisionResult | null;
  error: string | null;
}

interface ApiErrorBody {
  message?: string;
  errorCode?: string;
}

@Component({
  selector: 'app-vision-page',
  imports: [SegmentedControlComponent, DetectionOverlayComponent],
  templateUrl: './vision.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class VisionPageComponent {
  private readonly api = inject(ApiMediaService);
  protected readonly i18n = inject(I18nService);

  readonly activeTask = signal<TaskType>('caption');
  readonly tabStates = signal<Record<TaskType, TabState>>({
    caption: { image: null, file: null, result: null, error: null },
    detect: { image: null, file: null, result: null, error: null },
    ocr: { image: null, file: null, result: null, error: null },
  });

  readonly isLoading = signal(false);
  readonly zoomedImage = signal<string | null>(null);

  readonly taskOptions = computed(() => {
    const t = this.i18n.t().imageUploader;
    return [
      { value: 'caption' as TaskType, label: t.caption },
      { value: 'detect' as TaskType, label: t.detect },
      { value: 'ocr' as TaskType, label: t.ocr },
    ];
  });

  readonly currentState = computed<TabState>(() => this.tabStates()[this.activeTask()]);

  readonly processingTimeLabel = computed(() => {
    const ms = this.currentState().result?.processingTimeMs;
    if (ms == null) {
      return null;
    }
    return this.i18n.t().imageUploader.processingTime.replace('{ms}', String(ms));
  });

  setActiveTask(task: TaskType): void {
    this.activeTask.set(task);
  }

  onImageAreaClick(): void {
    if (!this.currentState().image) {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.onchange = e => this.onFileChange(e);
      input.click();
    }
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.processFile(file);
    }
    input.value = '';
  }

  private static readonly MAX_IMAGE_SIZE_BYTES = 50 * 1024 * 1024;

  processFile(file: File): void {
    if (!file.type.startsWith('image/')) {
      this.updateState({ error: this.i18n.t().imageUploader.selectImageError });
      return;
    }
    if (file.size > VisionPageComponent.MAX_IMAGE_SIZE_BYTES) {
      this.updateState({ error: this.i18n.t().imageUploader.fileTooLarge });
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      const imageData = e.target?.result as string;
      this.updateState({ image: imageData, file, error: null, result: null });
    };
    reader.readAsDataURL(file);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    const droppedFile = event.dataTransfer?.files[0];
    if (droppedFile) {
      this.processFile(droppedFile);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  clearImage(event: Event): void {
    event.stopPropagation();
    this.updateState({ image: null, file: null, error: null, result: null });
  }

  zoomImage(image: string): void {
    this.zoomedImage.set(image);
  }

  closeZoom(): void {
    this.zoomedImage.set(null);
  }

  submit(): void {
    const currentFile = this.currentState().file;
    if (!currentFile) return;

    this.isLoading.set(true);
    this.updateState({ error: null, result: null });

    const task = this.activeTask();
    let request: Observable<VisionResult>;
    switch (task) {
      case 'caption':
        request = this.api.captionImage(currentFile);
        break;
      case 'detect':
        request = this.api.detectObjects(currentFile);
        break;
      case 'ocr':
        request = this.api.ocrImage(currentFile);
        break;
    }

    request.subscribe({
      next: (data) => {
        this.updateState({ result: data });
      },
      error: (err: unknown) => {
        this.updateState({ error: this.resolveErrorMessage(err) });
        this.isLoading.set(false);
      },
      complete: () => {
        this.isLoading.set(false);
      },
    });
  }

  private resolveErrorMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      const body = err.error as ApiErrorBody | null;
      if (body?.errorCode === 'VISION_PROVIDER_UNAVAILABLE') {
        return this.i18n.t().imageUploader.providerUnavailable;
      }
      if (body?.message) {
        return body.message;
      }
      if (err.status === 0) {
        return this.i18n.t().imageUploader.requestFailed;
      }
    }
    if (err instanceof Error) {
      return err.message;
    }
    return this.i18n.t().imageUploader.processingFailed;
  }

  private updateState(partial: Partial<TabState>): void {
    this.tabStates.update(states => ({
      ...states,
      [this.activeTask()]: { ...states[this.activeTask()], ...partial },
    }));
  }
}
