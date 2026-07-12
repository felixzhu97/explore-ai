import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, catchError, of } from 'rxjs';
import type {
  ImageGenerateParams,
  ImageGenerationApiResponse,
  ImageGenerationResult,
  ImageCatalogResponse,
  TtsRequest,
  VisionResult,
  VisionHealthResponse,
  Voice,
} from '@shared/models';
import { API_BASE_URL, DEFAULT_VOICES } from './api.constants';

@Injectable({ providedIn: 'root' })
export class ApiMediaService {
  private http = inject(HttpClient);

  generateImage(params: ImageGenerateParams): Observable<ImageGenerationResult> {
    return this.http.post<ImageGenerationApiResponse>(
      `${API_BASE_URL}/images/generate`,
      {
        prompt: params.prompt,
        model: params.model,
        quality: params.quality,
        width: params.width,
        height: params.height,
        n: params.n ?? 1,
      },
    ).pipe(
      map(response => ({
        imageUrl: response.imageUrl ?? undefined,
        imageBase64: response.imageBase64 ?? undefined,
        model: response.model,
        prompt: response.prompt,
      })),
    );
  }

  getImageModels(): Observable<string[]> {
    return this.http
      .get<{ models: string[] }>(`${API_BASE_URL}/images/models`)
      .pipe(map(response => response.models ?? []));
  }

  getImageSizes(): Observable<string[]> {
    return this.http
      .get<{ sizes: string[] }>(`${API_BASE_URL}/images/sizes`)
      .pipe(map(response => response.sizes ?? []));
  }

  getImageQualities(): Observable<string[]> {
    return this.http
      .get<{ qualities: string[] }>(`${API_BASE_URL}/images/qualities`)
      .pipe(map(response => response.qualities ?? []));
  }

  getImageCatalog(): Observable<ImageCatalogResponse> {
    return forkJoin({
      models: this.getImageModels().pipe(catchError(() => of([] as string[]))),
      sizes: this.getImageSizes().pipe(catchError(() => of([] as string[]))),
      qualities: this.getImageQualities().pipe(catchError(() => of([] as string[]))),
    });
  }

  getVisionHealth(): Observable<VisionHealthResponse> {
    return this.http.get<VisionHealthResponse>(`${API_BASE_URL}/vision/health`);
  }

  captionImage(file: File): Observable<Pick<VisionResult, 'caption' | 'processingTimeMs'>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Pick<VisionResult, 'caption' | 'processingTimeMs'>>(
      `${API_BASE_URL}/vision/caption`,
      formData,
    );
  }

  detectObjects(file: File): Observable<Pick<VisionResult, 'detections' | 'processingTimeMs'>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Pick<VisionResult, 'detections' | 'processingTimeMs'>>(
      `${API_BASE_URL}/vision/detect`,
      formData,
    );
  }

  ocrImage(file: File): Observable<Pick<VisionResult, 'fullText' | 'processingTimeMs'>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Pick<VisionResult, 'fullText' | 'processingTimeMs'>>(
      `${API_BASE_URL}/vision/ocr`,
      formData,
    );
  }

  getVoices(): Observable<Voice[]> {
    return this.http
      .get<{ voices: (Voice | string)[] }>(`${API_BASE_URL}/audio/voices`)
      .pipe(
        map(response => this.normalizeVoices(response.voices)),
        catchError(() => of(DEFAULT_VOICES)),
      );
  }

  synthesizeSpeech(params: TtsRequest): Observable<Blob> {
    return this.http.post<Blob>(
      `${API_BASE_URL}/audio/speak`,
      {
        text: params.text,
        voice: params.voice,
        speed: params.speed,
        outputFormat: params.outputFormat,
      },
      {
        responseType: 'blob' as 'json',
      },
    );
  }

  downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  downloadBase64Image(base64: string, filename = 'image.png'): void {
    const mimeType = base64.startsWith('/9j/') ? 'image/jpeg' : 'image/png';
    const blob = this.base64ToBlob(base64, mimeType);
    this.downloadBlob(blob, filename);
  }

  base64ToBlob(base64: string, mimeType = 'image/png'): Blob {
    const byteCharacters = atob(base64);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    return new Blob([byteArray], { type: mimeType });
  }

  private normalizeVoices(voices: (Voice | string)[]): Voice[] {
    if (!voices?.length) {
      return DEFAULT_VOICES;
    }

    return voices.map((voice, index) => {
      if (typeof voice === 'string') {
        return {
          id: voice,
          name: voice.charAt(0).toUpperCase() + voice.slice(1),
          language: 'en',
          provider: 'openai',
          isDefault: index === 0,
        };
      }
      return {
        ...voice,
        provider: voice.provider ?? 'openai',
        isDefault: voice.isDefault ?? index === 0,
      };
    });
  }
}
