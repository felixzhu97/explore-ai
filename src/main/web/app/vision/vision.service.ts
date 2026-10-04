import { Service, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { type Observable, map } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import type { AppError } from '../http/http-error.interceptor';
import { I18nService } from '../i18n';
import { ImageZoomService } from '../ui/image-zoom.service';

export interface CaptionResponse {
  caption: string;
  processingTimeMs: number;
}

export interface DetectionResponse {
  className: string;
  confidence: number;
  /** `[x, y, width, height]` in source image pixels. */
  bbox: number[];
}

export interface DetectResponse {
  detections: DetectionResponse[];
  processingTimeMs: number;
}

export interface OcrResponse {
  fullText: string;
  processingTimeMs: number;
}

export type VisionTaskType = 'caption' | 'detect' | 'ocr';

export type VisionResult =
  | ({ task: 'caption' } & CaptionResponse)
  | ({ task: 'detect' } & DetectResponse)
  | ({ task: 'ocr' } & OcrResponse);

export interface VisionTabState {
  image: string | null;
  file: File | null;
  result: VisionResult | null;
  error: string | null;
}

const MAX_IMAGE_SIZE_BYTES = 50 * 1024 * 1024;

@Service()
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
    if (ms === undefined) {
      return null;
    }
    return this.#i18n.t().vision.processingTime.replace('{ms}', String(ms));
  });

  readonly detections = computed(() => {
    const result = this.currentState().result;
    return result?.task === 'detect' ? result.detections : undefined;
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
    if (currentFile === null || this.isLoading()) {
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

  #captionImage(file: File): Observable<VisionResult> {
    return this.#http
      .post<CaptionResponse>(`${API_BASE_URL}/vision/caption`, imageForm(file))
      .pipe(map(response => ({ task: 'caption', ...response })));
  }

  #detectObjects(file: File): Observable<VisionResult> {
    return this.#http
      .post<DetectResponse>(`${API_BASE_URL}/vision/detect`, imageForm(file))
      .pipe(map(response => ({ task: 'detect', ...response })));
  }

  #ocrImage(file: File): Observable<VisionResult> {
    return this.#http
      .post<OcrResponse>(`${API_BASE_URL}/vision/ocr`, imageForm(file))
      .pipe(map(response => ({ task: 'ocr', ...response })));
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

function imageForm(file: File): FormData {
  const formData = new FormData();
  formData.append('file', file);
  return formData;
}
