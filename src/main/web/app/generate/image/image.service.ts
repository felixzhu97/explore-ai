import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../../core/api.constants';
import { ImageZoomService } from '../../shared/services/image-zoom.service';
import { downloadBase64Image, downloadBlob } from '../../shared/utils/download';
import {
  DEFAULT_IMAGE_SIZES,
  parseImageSizeLabel,
  type ImageCatalogResponse,
  type ImageGenerateParams,
  type ImageGenerationApiResponse,
  type ImageGenerationResult,
  type ImageSize,
} from './image.model';

@Injectable({ providedIn: 'root' })
export class ImageService {
  private readonly http = inject(HttpClient);
  private readonly imageZoom = inject(ImageZoomService);

  readonly prompt = signal('');
  readonly isGenerating = signal(false);
  readonly error = signal<string | null>(null);
  readonly generatedImage = signal<string | null>(null);
  readonly models = signal<string[]>([]);
  readonly qualities = signal<string[]>([]);
  readonly sizes = signal<ImageSize[]>(DEFAULT_IMAGE_SIZES);
  readonly selectedModel = signal<string | null>(null);
  readonly selectedQuality = signal<string | null>(null);
  readonly selectedSize = signal<ImageSize>(DEFAULT_IMAGE_SIZES[2]);

  private readonly imageSource = signal<'url' | 'base64' | null>(null);

  readonly hasGeneratedImage = computed(() => Boolean(this.generatedImage()));

  constructor() {
    this.loadCatalog();
  }

  loadCatalog(): void {
    this.getImageCatalog().subscribe({
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
        if (parsedSizes.length > 0) {
          this.sizes.set(parsedSizes);
          this.selectedSize.set(parsedSizes[parsedSizes.length - 1]);
        }
      },
    });
  }

  setPrompt(text: string): void {
    this.prompt.set(text);
  }

  setSize(size: ImageSize): void {
    this.selectedSize.set(size);
  }

  openZoom(): void {
    const image = this.generatedImage();
    if (image) {
      this.imageZoom.open(image);
    }
  }

  generate(): void {
    if (!this.prompt().trim() || this.isGenerating()) {
      return;
    }

    this.isGenerating.set(true);
    this.error.set(null);
    this.generatedImage.set(null);
    this.imageSource.set(null);

    const size = this.selectedSize();
    this.generateImage({
      prompt: this.prompt(),
      model: this.selectedModel() ?? undefined,
      quality: this.selectedQuality() ?? undefined,
      width: size.width,
      height: size.height,
      n: 1,
    }).subscribe({
      next: (result) => {
        if (result.imageUrl) {
          this.generatedImage.set(result.imageUrl);
          this.imageSource.set('url');
          return;
        }
        if (result.imageBase64) {
          this.generatedImage.set(`data:image/png;base64,${result.imageBase64}`);
          this.imageSource.set('base64');
        }
      },
      error: (err: unknown) => {
        this.error.set(this.extractErrorMessage(err));
        this.isGenerating.set(false);
      },
      complete: () => {
        this.isGenerating.set(false);
      },
    });
  }

  download(): void {
    const image = this.generatedImage();
    if (!image) {
      return;
    }

    const filename = `ai_generated_${Date.now()}.png`;
    if (this.imageSource() === 'base64') {
      const base64 = image.replace(/^data:image\/\w+;base64,/, '');
      downloadBase64Image(base64, filename);
      return;
    }

    fetch(image)
      .then(response => response.blob())
      .then(blob => downloadBlob(blob, filename))
      .catch(() => this.error.set('Failed to download image'));
  }

  private generateImage(params: ImageGenerateParams): Observable<ImageGenerationResult> {
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

  private getImageModels(): Observable<string[]> {
    return this.http
      .get<{ models: string[] }>(`${API_BASE_URL}/images/models`)
      .pipe(map(response => response.models ?? []));
  }

  private getImageSizes(): Observable<string[]> {
    return this.http
      .get<{ sizes: string[] }>(`${API_BASE_URL}/images/sizes`)
      .pipe(map(response => response.sizes ?? []));
  }

  private getImageQualities(): Observable<string[]> {
    return this.http
      .get<{ qualities: string[] }>(`${API_BASE_URL}/images/qualities`)
      .pipe(map(response => response.qualities ?? []));
  }

  private getImageCatalog(): Observable<ImageCatalogResponse> {
    return forkJoin({
      models: this.getImageModels().pipe(catchError(() => of([] as string[]))),
      sizes: this.getImageSizes().pipe(catchError(() => of([] as string[]))),
      qualities: this.getImageQualities().pipe(catchError(() => of([] as string[]))),
    });
  }

  private extractErrorMessage(err: unknown): string {
    if (err instanceof Error) {
      return err.message;
    }
    if (typeof err === 'object' && err !== null && 'error' in err) {
      const errorBody = (err as { error?: { status?: string } }).error;
      if (errorBody?.status?.startsWith('ERROR:')) {
        return errorBody.status.replace(/^ERROR:\s*/, '');
      }
    }
    return 'Image generation failed';
  }
}
