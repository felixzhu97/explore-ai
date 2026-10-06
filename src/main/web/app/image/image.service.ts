import { Service, inject, signal, computed } from '@angular/core';
import { Instant } from '@js-joda/core';
import { HttpClient } from '@angular/common/http';
import { type Observable, forkJoin, map, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import type { AppError } from '../http/http-error.interceptor';
import { ImageZoomService } from '../ui/image-zoom.service';
import { downloadBase64Image, downloadBlob } from '../ui/download';
import { hasText } from '../shared/presence';

export interface ImageSize {
  label: string;
  width: number;
  height: number;
}

/** POST /api/images/generate */
export interface ImageGenerationRequest {
  prompt: string;
  model?: string;
  quality?: string;
  width?: number;
  height?: number;
  n?: number;
}

export type ImageGenerationStatus = 'SUCCESS';

export interface ImageGenerationResponse {
  imageUrl: string | null;
  imageBase64: string | null;
  model: string | null;
  prompt: string;
  revisedPrompt: string | null;
  status: ImageGenerationStatus;
}

export interface ImageModelsResponse {
  models: string[];
}

export interface ImageSizesResponse {
  sizes: string[];
}

export interface ImageQualitiesResponse {
  qualities: string[];
}

/** Models, sizes and qualities fetched together; empty lists when a lookup fails. */
export interface ImageCatalog {
  models: string[];
  sizes: string[];
  qualities: string[];
}

/** Parses a size label such as 1024x1024. */
export function parseImageSizeLabel(label: string): ImageSize | null {
  const [, width, height] = /^(\d+)x(\d+)$/.exec(label.trim()) ?? [];
  if (!hasText(width) || !hasText(height)) {
    return null;
  }
  return {
    label,
    width: Number.parseInt(width, 10),
    height: Number.parseInt(height, 10),
  };
}

const DEFAULT_IMAGE_SIZE: ImageSize = { label: '1024x1024', width: 1024, height: 1024 };

export const DEFAULT_IMAGE_SIZES: ImageSize[] = [
  { label: '512x512', width: 512, height: 512 },
  { label: '768x768', width: 768, height: 768 },
  DEFAULT_IMAGE_SIZE,
];

@Service()
export class ImageService {
  readonly #http = inject(HttpClient);
  readonly #imageZoom = inject(ImageZoomService);

  readonly prompt = signal('');
  readonly isGenerating = signal(false);
  readonly error = signal<string | null>(null);
  readonly generatedImage = signal<string | null>(null);
  readonly models = signal<string[]>([]);
  readonly qualities = signal<string[]>([]);
  readonly sizes = signal<ImageSize[]>(DEFAULT_IMAGE_SIZES);
  readonly selectedModel = signal<string | undefined>(undefined);
  readonly selectedQuality = signal<string | undefined>(undefined);
  readonly selectedSize = signal<ImageSize>(DEFAULT_IMAGE_SIZE);

  readonly #imageSource = signal<'url' | 'base64' | null>(null);

  readonly hasGeneratedImage = computed(() => hasText(this.generatedImage()));

  constructor() {
    this.loadCatalog();
  }

  /** Loads the image models, sizes and qualities. */
  loadCatalog(): void {
    this.#getImageCatalog().subscribe({
      next: (catalog) => {
        if (catalog.models.length > 0) {
          this.models.set(catalog.models);
          this.selectedModel.set(catalog.models[0]);
        }
        if (catalog.qualities.length > 0) {
          this.qualities.set(catalog.qualities);
          this.selectedQuality.set(catalog.qualities[0]);
        }
        const parsedSizes = catalog.sizes
          .map(parseImageSizeLabel)
          .filter((size): size is ImageSize => size !== null);
        const largest = parsedSizes.at(-1);
        if (largest !== undefined) {
          this.sizes.set(parsedSizes);
          this.selectedSize.set(largest);
        }
      },
    });
  }

  /** Sets the image prompt. */
  setPrompt(text: string): void {
    this.prompt.set(text);
  }

  /** Selects the image size. */
  setSize(size: ImageSize): void {
    this.selectedSize.set(size);
  }

  /** Opens the generated image in the zoom view. */
  openZoom(): void {
    const image = this.generatedImage();
    if (hasText(image)) {
      this.#imageZoom.open(image);
    }
  }

  /** Generates an image from the prompt. */
  generate(): void {
    if (this.prompt().trim() === '' || this.isGenerating()) {
      return;
    }

    this.isGenerating.set(true);
    this.error.set(null);
    this.generatedImage.set(null);
    this.#imageSource.set(null);

    const size = this.selectedSize();
    const model = this.selectedModel();
    const quality = this.selectedQuality();
    this.#generateImage({
      prompt: this.prompt(),
      ...(model !== undefined ? { model } : {}),
      ...(quality !== undefined ? { quality } : {}),
      width: size.width,
      height: size.height,
      n: 1,
    }).subscribe({
      next: (result) => {
        if (hasText(result.imageUrl)) {
          this.generatedImage.set(result.imageUrl);
          this.#imageSource.set('url');
          return;
        }
        if (hasText(result.imageBase64)) {
          this.generatedImage.set(`data:image/png;base64,${result.imageBase64}`);
          this.#imageSource.set('base64');
        }
      },
      error: (error: AppError) => {
        this.error.set(error.message);
        this.isGenerating.set(false);
      },
      complete: () => {
        this.isGenerating.set(false);
      },
    });
  }

  /** Downloads the generated image. */
  download(): void {
    const image = this.generatedImage();
    if (!hasText(image)) {
      return;
    }

    const filename = `ai_generated_${String(Instant.now().toEpochMilli())}.png`;
    if (this.#imageSource() === 'base64') {
      const base64 = image.replace(/^data:image\/\w+;base64,/, '');
      downloadBase64Image(base64, filename);
      return;
    }

    fetch(image)
      .then(response => response.blob())
      .then(blob => downloadBlob(blob, filename))
      .catch(() => this.error.set('Failed to download image'));
  }

  #generateImage(request: ImageGenerationRequest): Observable<ImageGenerationResponse> {
    return this.#http.post<ImageGenerationResponse>(`${API_BASE_URL}/images/generate`, request);
  }

  #getImageModels(): Observable<string[]> {
    return this.#http
      .get<ImageModelsResponse>(`${API_BASE_URL}/images/models`)
      .pipe(map(response => response.models));
  }

  #getImageSizes(): Observable<string[]> {
    return this.#http
      .get<ImageSizesResponse>(`${API_BASE_URL}/images/sizes`)
      .pipe(map(response => response.sizes));
  }

  #getImageQualities(): Observable<string[]> {
    return this.#http
      .get<ImageQualitiesResponse>(`${API_BASE_URL}/images/qualities`)
      .pipe(map(response => response.qualities));
  }

  #getImageCatalog(): Observable<ImageCatalog> {
    return forkJoin({
      models: this.#getImageModels().pipe(catchError(() => of([] as string[]))),
      sizes: this.#getImageSizes().pipe(catchError(() => of([] as string[]))),
      qualities: this.#getImageQualities().pipe(catchError(() => of([] as string[]))),
    });
  }
}
