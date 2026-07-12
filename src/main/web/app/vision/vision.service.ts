import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiMediaService } from '@core/services/api-media.service';
import { I18nService } from '@core/i18n';
import { ImageZoomService } from '@shared/services/image-zoom.service';
import type { VisionResult } from './vision.model';

export type VisionTaskType = 'caption' | 'detect' | 'ocr';

export interface VisionTabState {
  image: string | null;
  file: File | null;
  result: VisionResult | null;
  error: string | null;
}

interface ApiErrorBody {
  message?: string;
  errorCode?: string;
}

const MAX_IMAGE_SIZE_BYTES = 50 * 1024 * 1024;

@Injectable({ providedIn: 'root' })
export class VisionService {
  private readonly api = inject(ApiMediaService);
  private readonly i18n = inject(I18nService);
  private readonly imageZoom = inject(ImageZoomService);

  readonly activeTask = signal<VisionTaskType>('caption');
  readonly tabStates = signal<Record<VisionTaskType, VisionTabState>>({
    caption: { image: null, file: null, result: null, error: null },
    detect: { image: null, file: null, result: null, error: null },
    ocr: { image: null, file: null, result: null, error: null },
  });

  readonly isLoading = signal(false);

  readonly currentState = computed<VisionTabState>(
    () => this.tabStates()[this.activeTask()],
  );

  readonly processingTimeLabel = computed(() => {
    const ms = this.currentState().result?.processingTimeMs;
    if (ms == null) {
      return null;
    }
    return this.i18n.t().imageUploader.processingTime.replace('{ms}', String(ms));
  });

  readonly canAnalyze = computed(() => Boolean(this.currentState().file));

  setActiveTask(task: VisionTaskType): void {
    this.activeTask.set(task);
  }

  processFile(file: File): void {
    if (!file.type.startsWith('image/')) {
      this.updateState({ error: this.i18n.t().imageUploader.selectImageError });
      return;
    }
    if (file.size > MAX_IMAGE_SIZE_BYTES) {
      this.updateState({ error: this.i18n.t().imageUploader.fileTooLarge });
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      const imageData = event.target?.result as string;
      this.updateState({ image: imageData, file, error: null, result: null });
    };
    reader.readAsDataURL(file);
  }

  clearImage(): void {
    this.updateState({ image: null, file: null, error: null, result: null });
  }

  openZoom(image: string): void {
    this.imageZoom.open(image, this.i18n.t().imageUploader.imageLabel);
  }

  analyze(): void {
    const currentFile = this.currentState().file;
    if (!currentFile || this.isLoading()) {
      return;
    }

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

  private updateState(partial: Partial<VisionTabState>): void {
    this.tabStates.update(states => ({
      ...states,
      [this.activeTask()]: { ...states[this.activeTask()], ...partial },
    }));
  }
}
