import { Component, signal, inject, ChangeDetectionStrategy, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@core/i18n';

interface ImageSize {
  label: string;
  width: number;
  height: number;
}

@Component({
  selector: 'app-image-gen-tab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">{{ i18n.t().aiHub.image.title }}</h3>
          <p class="panel-description">
            {{ i18n.t().aiHub.image.description }}
          </p>
        </div>
      </div>
      <div class="panel-content">
        <div class="image-section">
          <!-- Prompt Area -->
          <div class="prompt-area">
            <div class="input-group">
              <label class="input-label">{{ i18n.t().aiHub.image.promptLabel }}</label>
              <textarea
                class="text-input"
                [ngModel]="prompt()"
                (ngModelChange)="setPrompt($event)"
                placeholder="{{ i18n.t().aiHub.image.promptPlaceholder }}"
                rows="4"
              ></textarea>
            </div>

            <div class="input-group">
              <label class="input-label">{{ i18n.t().aiHub.image.negativePromptLabel }}</label>
              <textarea
                class="text-input"
                [ngModel]="negativePrompt()"
                (ngModelChange)="setNegativePrompt($event)"
                placeholder="{{ i18n.t().aiHub.image.negativePromptPlaceholder }}"
                rows="2"
              ></textarea>
            </div>

            <div class="input-group">
              <label class="input-label">{{ i18n.t().aiHub.image.sizeLabel }}</label>
              <div class="size-selector">
                @for (size of sizes; track size.label) {
                  <button
                    class="size-option"
                    [class.selected]="selectedSize().label === size.label"
                    (click)="setSize(size)"
                  >
                    {{ size.label }}
                  </button>
                }
              </div>
            </div>

            <button
              class="action-button primary"
              (click)="generate()"
              [disabled]="!prompt().trim() || isGenerating()"
            >
              @if (isGenerating()) {
                <span class="btn-spinner"></span>
                {{ i18n.t().aiHub.image.generating }}
              } @else {
                {{ i18n.t().aiHub.image.generateButton }}
              }
            </button>
          </div>

          <!-- Preview Area -->
          <div class="preview-area">
            <div class="image-area">
              @if (isGenerating()) {
                <div class="loading-overlay">
                  <div class="loading-spinner"></div>
                  <span class="loading-text">Generating...</span>
                </div>
              }
              @if (generatedImage()) {
                <img
                  class="generated-image"
                  [src]="generatedImage()"
                  alt="Generated"
                  (click)="zoom.emit(generatedImage()!)"
                />
              } @else {
                <div class="empty-state">
                  <div class="empty-icon">
                    <svg
                      width="48"
                      height="48"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.5"
                    >
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                      <circle cx="8.5" cy="8.5" r="1.5" />
                      <polyline points="21 15 16 10 5 21" />
                    </svg>
                  </div>
                  <p class="empty-title">
                    {{ i18n.t().aiHub.image.emptyState }}
                  </p>
                </div>
              }
            </div>

            @if (error()) {
              <div class="error-message">{{ error() }}</div>
            }

            @if (generatedImage()) {
              <div class="image-actions">
                <button class="icon-button" (click)="download()">
                  ⬇️ {{ i18n.t().aiHub.image.download }}
                </button>
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .panel {
        display: flex;
        flex-direction: column;
        gap: 16px;
      }

      .panel-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 16px;
        background: #ffffff;
        border: 1px solid var(--color-border);
        border-radius: 14px;
      }

      .panel-title {
        font-size: 17px;
        font-weight: 600;
        color: #1d1d1f;
        margin: 0;
      }

      .panel-description {
        font-size: 14px;
        color: #86868b;
        margin: 4px 0 0 0;
      }

      .panel-content {
        background: #ffffff;
        border: 1px solid var(--color-border);
        border-radius: 14px;
        padding: 24px;
      }

      .image-section {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 24px;
      }

      @media (max-width: 768px) {
        .image-section {
          grid-template-columns: 1fr;
        }
      }

      .prompt-area {
        display: flex;
        flex-direction: column;
        gap: 16px;
      }

      .preview-area {
        display: flex;
        flex-direction: column;
        gap: 12px;
        min-height: 300px;
      }

      .input-group {
        display: flex;
        flex-direction: column;
        gap: 6px;
      }

      .input-label {
        font-size: 14px;
        font-weight: 500;
        color: #86868b;
      }

      .text-input {
        padding: 12px 16px;
        font-size: 15px;
        font-family: inherit;
        border: 1px solid var(--color-border);
        border-radius: 10px;
        background: #ffffff;
        color: #1d1d1f;
        resize: vertical;
        min-height: 80px;
        transition:
          border-color 0.15s,
          box-shadow 0.15s;
      }

      .text-input:focus {
        outline: none;
        border-color: #007aff;
        box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.2);
      }

      .text-input::placeholder {
        color: #86868b;
      }

      .size-selector {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }

      .size-option {
        padding: 8px 16px;
        font-size: 14px;
        font-weight: 500;
        background: #ffffff;
        color: #1d1d1f;
        border: 1px solid var(--color-border);
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.15s ease;
      }

      .size-option:hover {
        border-color: #007aff;
      }

      .size-option.selected {
        background: #007aff;
        color: white;
        border: transparent;
      }

      .action-button {
        padding: 12px 24px;
        font-size: 15px;
        font-weight: 500;
        font-family: inherit;
        background: #ffffff;
        color: #1d1d1f;
        border: 1px solid var(--color-border);
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.15s ease;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
      }

      .action-button.primary {
        background: #007aff;
        color: white;
        border: transparent;
      }

      .action-button.primary:hover:not(:disabled) {
        background: #0071e3;
      }

      .action-button:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      .image-area {
        flex: 1;
        position: relative;
        border-radius: 10px;
        background: #f5f5f7;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        min-height: 256px;
        border: 2px dashed rgba(0, 0, 0, 0.08);
      }

      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 48px;
        color: #6e6e73;
        text-align: center;
        gap: 8px;
      }

      .empty-icon {
        font-size: 48px;
        opacity: 0.5;
      }

      .empty-title {
        font-size: 16px;
        font-weight: 500;
        color: #1d1d1f;
        margin: 0;
      }

      .generated-image {
        max-width: 100%;
        max-height: 400px;
        object-fit: contain;
        border-radius: 10px;
        cursor: zoom-in;
        animation: fadeIn 0.3s ease;
      }

      .generated-image:hover {
        transform: scale(1.02);
      }

      .loading-overlay {
        position: absolute;
        inset: 0;
        background: rgba(255, 255, 255, 0.9);
        backdrop-filter: blur(8px);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 12px;
        border-radius: 12px;
        z-index: 5;
      }

      .loading-spinner {
        width: 44px;
        height: 44px;
        border: 3px solid #f5f5f7;
        border-top-color: #0071e3;
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }

      .loading-text {
        font-size: 14px;
        font-weight: 500;
        color: #6e6e73;
      }

      .image-actions {
        display: flex;
        gap: 8px;
        justify-content: center;
      }

      .icon-button {
        padding: 8px 16px;
        font-size: 14px;
        font-weight: 500;
        background: #ffffff;
        color: #0071e3;
        border: 1px solid #e5e5e5;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.15s ease;
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .icon-button:hover {
        background: #f5f5f7;
        border-color: #0071e3;
      }

      .error-message {
        padding: 12px;
        background: #ffebee;
        color: #c62828;
        border-radius: 8px;
        font-size: 14px;
        animation: fadeIn 0.2s ease;
        border: 1px solid #ffcdd2;
      }

      .btn-spinner {
        display: inline-block;
        width: 16px;
        height: 16px;
        border: 2px solid rgba(255, 255, 255, 0.3);
        border-top-color: white;
        border-radius: 50%;
        animation: spin 0.7s linear infinite;
      }

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
    `,
  ],
})
export class ImageGenTabComponent {
  private readonly api = inject(ApiService);
  protected readonly i18n = inject(I18nService);

  zoom = output<string>();

  prompt = signal('');
  negativePrompt = signal('');
  isGenerating = signal(false);
  error = signal<string | null>(null);
  generatedImage = signal<string | null>(null);

  sizes: ImageSize[] = [
    { label: '512x512', width: 512, height: 512 },
    { label: '768x768', width: 768, height: 768 },
    { label: '1024x1024', width: 1024, height: 1024 },
  ];
  selectedSize = signal<ImageSize>(this.sizes[2]);

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
