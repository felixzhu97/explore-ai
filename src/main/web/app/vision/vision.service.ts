import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { type Observable } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import type { AppError } from '../http/http-error.interceptor';
import { I18nService } from '../i18n';
import { ImageZoomService } from '../ui/image-zoom.service';
import type { Detection } from './detection-overlay.component';

export interface VisionResult {
  caption?: string;
  detections?: Detection[];
  fullText?: string;
  processingTimeMs?: number;
}

export type VisionTaskType = 'caption' | 'detect' | 'ocr';

export interface VisionTabState {
  image: string | null;
  file: File | null;
  result: VisionResult | null;
  error: string | null;
}

const MAX_IMAGE_SIZE_BYTES = 50 * 1024 * 1024;

@Injectable({ providedIn: 'root' })
export class VisionService {
  readonly #http = inject(HttpClient);
  readonly #i18n = inject(I18nService);
  readonly #imageZoom = inject(ImageZoomService);

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
    return this.#i18n.t().vision.processingTime.replace('{ms}', String(ms));
  });

  readonly canAnalyze = computed(() => Boolean(this.currentState().file));

  setActiveTask(task: VisionTaskType): void {
    this.activeTask.set(task);
  }

  processFile(file: File): void {
    if (!file.type.startsWith('image/')) {
      this.#updateState({ error: this.#i18n.t().vision.errors.invalidImage });
      return;
    }
    if (file.size > MAX_IMAGE_SIZE_BYTES) {
      this.#updateState({ error: this.#i18n.t().vision.errors.fileTooLarge });
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      const imageData = event.target?.result as string;
      this.#updateState({ image: imageData, file, error: null, result: null });
    };
    reader.readAsDataURL(file);
  }

  clearImage(): void {
    this.#updateState({ image: null, file: null, error: null, result: null });
  }

  openZoom(image: string): void {
    this.#imageZoom.open(image, this.#i18n.t().vision.imageLabel);
  }

  analyze(): void {
    const currentFile = this.currentState().file;
    if (!currentFile || this.isLoading()) {
      return;
    }

    this.isLoading.set(true);
    this.#updateState({ error: null, result: null });

    const task = this.activeTask();
    let request: Observable<VisionResult>;
    switch (task) {
      case 'caption':
        request = this.#captionImage(currentFile);
        break;
      case 'detect':
        request = this.#detectObjects(currentFile);
        break;
      case 'ocr':
        request = this.#ocrImage(currentFile);
        break;
    }

    request.subscribe({
      next: (data) => {
        this.#updateState({ result: data });
      },
      error: (error: AppError) => {
        this.#updateState({ error: this.#resolveErrorMessage(error) });
        this.isLoading.set(false);
      },
      complete: () => {
        this.isLoading.set(false);
      },
    });
  }

  #captionImage(file: File): Observable<Pick<VisionResult, 'caption' | 'processingTimeMs'>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.#http.post<Pick<VisionResult, 'caption' | 'processingTimeMs'>>(
      `${API_BASE_URL}/vision/caption`,
      formData,
    );
  }

  #detectObjects(file: File): Observable<Pick<VisionResult, 'detections' | 'processingTimeMs'>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.#http.post<Pick<VisionResult, 'detections' | 'processingTimeMs'>>(
      `${API_BASE_URL}/vision/detect`,
      formData,
    );
  }

  #ocrImage(file: File): Observable<Pick<VisionResult, 'fullText' | 'processingTimeMs'>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.#http.post<Pick<VisionResult, 'fullText' | 'processingTimeMs'>>(
      `${API_BASE_URL}/vision/ocr`,
      formData,
    );
  }

  #resolveErrorMessage(error: AppError): string {
    if (error.errorCode === 'VISION_PROVIDER_UNAVAILABLE') {
      return this.#i18n.t().vision.errors.providerUnavailable;
    }
    if (error.status === 0) {
      return this.#i18n.t().vision.errors.requestFailed;
    }
    return error.message;
  }

  #updateState(partial: Partial<VisionTabState>): void {
    this.tabStates.update(states => ({
      ...states,
      [this.activeTask()]: { ...states[this.activeTask()], ...partial },
    }));
  }
}
