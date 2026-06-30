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

    this.api
      .generateImage({
        prompt: this.prompt(),
        negative_prompt: this.negativePrompt() || undefined,
        width: this.selectedSize().width,
        height: this.selectedSize().height,
        num_images: 1,
      })
      .subscribe({
        next: (result) => {
          if (result.images && result.images.length > 0) {
            this.generatedImage.set(`data:image/png;base64,${result.images[0]}`);
          }
        },
        error: (err: unknown) => {
          this.error.set(err instanceof Error ? err.message : 'Image generation failed');
          this.isGenerating.set(false);
        },
        complete: () => {
          this.isGenerating.set(false);
        },
      });
  }

  download() {
    if (this.generatedImage()) {
      const base64 = this.generatedImage()!.replace('data:image/png;base64,', '');
      this.api.downloadBase64Image(base64, `ai_generated_${Date.now()}.png`);
    }
  }
}
