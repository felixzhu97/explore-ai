import { Component, signal, inject, ChangeDetectionStrategy, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@core/i18n';
import type { ImageSize } from '../image.model';

@Component({
  selector: 'app-image-gen-tab',
  imports: [FormsModule],
  standalone: true,
  templateUrl: './image.component.html',

  styles: [`
    @keyframes fadeIn {
      from {
        opacity: 0;
        transform: translateY(8px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes spin {
      from {
        transform: rotate(0deg);
      }
      to {
        transform: rotate(360deg);
      }
    }

    .animate-fade-in {
      animation: fadeIn 0.3s ease;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block min-w-0 w-full max-w-full' },
})
export class ImageComponent {
  private readonly api = inject(ApiService);
  protected readonly i18n = inject(I18nService);

  zoom = output<string>();

  readonly prompt = signal('');
  readonly negativePrompt = signal('');
  readonly isGenerating = signal(false);
  readonly error = signal<string | null>(null);
  readonly generatedImage = signal<string | null>(null);
  private readonly imageSource = signal<'url' | 'base64' | null>(null);

  sizes: ImageSize[] = [
    { label: '512x512', width: 512, height: 512 },
    { label: '768x768', width: 768, height: 768 },
    { label: '1024x1024', width: 1024, height: 1024 },
  ];

  readonly selectedSize = signal<ImageSize>(this.sizes[2]);

  setPrompt(text: string) {
    this.prompt.set(text);
  }

  setNegativePrompt(text: string) {
    this.negativePrompt.set(text);
  }

  setSize(size: ImageSize) {
    this.selectedSize.set(size);
  }

  generate() {
    if (!this.prompt().trim() || this.isGenerating()) return;

    this.isGenerating.set(true);
    this.error.set(null);
    this.generatedImage.set(null);
    this.imageSource.set(null);

    this.api
      .generateImage({
        prompt: this.prompt(),
        width: this.selectedSize().width,
        height: this.selectedSize().height,
        n: 1,
      })
      .subscribe({
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
          const message = this.extractErrorMessage(err);
          this.error.set(message);
          this.isGenerating.set(false);
        },
        complete: () => {
          this.isGenerating.set(false);
        },
      });
  }

  download() {
    const image = this.generatedImage();
    if (!image) {
      return;
    }

    const filename = `ai_generated_${Date.now()}.png`;
    if (this.imageSource() === 'base64') {
      const base64 = image.replace(/^data:image\/\w+;base64,/, '');
      this.api.downloadBase64Image(base64, filename);
      return;
    }

    fetch(image)
      .then(response => response.blob())
      .then(blob => this.api.downloadBlob(blob, filename))
      .catch(() => this.error.set('Failed to download image'));
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
