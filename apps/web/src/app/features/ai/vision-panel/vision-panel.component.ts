import { Component, signal, inject, ChangeDetectionStrategy, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { I18nService } from '../../../i18n';
import {
  SegmentedControlComponent,
  SegmentedControlOption,
} from '../../../shared/components/ui/segmented-control/segmented-control.component';
import { Detection, VisionResult } from '../../../shared/models';

type TaskType = 'caption' | 'detect' | 'ocr';

interface TabState {
  image: string | null;
  file: File | null;
  result: VisionResult | null;
  error: string | null;
}

@Component({
  selector: 'app-vision-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, SegmentedControlComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="vision-panel">
      <!-- Tab Navigation -->
      <div class="tab-header">
        <app-segmented-control
          [options]="taskOptions()"
          [value]="activeTask()"
          (changed)="setActiveTask($event)"
        />
      </div>

      <!-- Main Content -->
      <div class="main-area">
        <!-- Image Upload Panel -->
        <div class="panel">
          <h3 class="panel-label">{{ i18n.t().imageUploader.imageLabel }}</h3>
          <div
            class="image-area"
            (click)="onImageAreaClick()"
            (drop)="onDrop($event)"
            (dragover)="onDragOver($event)"
          >
            @if (currentState().image) {
              <img
                class="preview-image"
                [src]="currentState().image"
                alt="Preview"
                (click)="zoomImage(currentState().image!); $event.stopPropagation()"
              />
              <div class="zoom-hint">Click to enlarge</div>
              <button class="clear-button" (click)="clearImage($event)">×</button>
              @if (isLoading()) {
                <div class="loading-overlay">
                  <div class="spinner"></div>
                </div>
              }
            } @else {
              <div class="drop-zone">
                <div class="drop-icon">
                  <svg
                    width="32"
                    height="32"
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
                <p class="drop-text">{{ i18n.t().imageUploader.dropText }}</p>
                <p class="drop-hint">{{ i18n.t().imageUploader.dropHint }}</p>
              </div>
            }
          </div>
        </div>

        <!-- Result Panel -->
        <div class="panel">
          <h3 class="panel-label">{{ i18n.t().imageUploader.resultLabel }}</h3>
          <div class="result-content">
            @if (currentState().result) {
              @if (activeTask() === 'caption') {
                <p class="result-text">"{{ currentState().result!.caption }}"</p>
              }
              @if (activeTask() === 'detect') {
                <div class="detection-list">
                  @for (det of currentState().result!.detections; track $index) {
                    <div class="detection-item">
                      <span class="detection-name">{{ det.class_name }}</span>
                      <span class="confidence">{{ (det.confidence * 100).toFixed(0) }}%</span>
                    </div>
                  }
                </div>
              }
              @if (activeTask() === 'ocr') {
                <pre class="ocr-text">{{ currentState().result!.full_text }}</pre>
              }
            } @else {
              <div class="empty-state">
                {{ i18n.t().imageUploader.noImageYet }}
              </div>
            }
          </div>

          @if (currentState().error) {
            <div class="error-message">{{ currentState().error }}</div>
          }

          <div class="action-area">
            <button
              class="action-button primary"
              (click)="submit()"
              [disabled]="!currentState().file || isLoading()"
            >
              @if (isLoading()) {
                <span class="btn-spinner"></span>
                {{ i18n.t().imageUploader.analyzing }}
              } @else {
                {{ i18n.t().imageUploader.startAnalyze }}
              }
            </button>
          </div>
        </div>
      </div>

      <!-- Zoom Modal -->
      @if (zoomedImage()) {
        <div class="zoom-modal" (click)="closeZoom()">
          <div class="zoom-content">
            <button class="zoom-close" (click)="closeZoom()">×</button>
            <img [src]="zoomedImage()" alt="Zoomed" />
          </div>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .vision-panel {
        display: flex;
        flex-direction: column;
        gap: 16px;
      }

      .tab-header {
        display: flex;
        justify-content: center;
        padding: 16px 0;
        overflow-x: auto;
        -webkit-overflow-scrolling: touch;
        scrollbar-width: none;
        -ms-overflow-style: none;

        &::-webkit-scrollbar {
          display: none;
        }
      }

      .main-area {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 24px;
        min-height: 480px;
      }

      @media (max-width: 640px) {
        .main-area {
          grid-template-columns: 1fr;
        }
      }

      .panel {
        background: #ffffff;
        border: 1px solid var(--color-border);
        border-radius: 14px;
        padding: 24px;
        display: flex;
        flex-direction: column;
        gap: 16px;
      }

      .panel-label {
        font-size: 12px;
        font-weight: 500;
        color: #86868b;
        margin: 0;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .image-area {
        flex: 1;
        position: relative;
        border-radius: 10px;
        background: rgba(0, 0, 0, 0.04);
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: background 0.15s ease;
        overflow: hidden;
        min-height: 300px;
      }

      .image-area:hover {
        background: rgba(0, 0, 0, 0.02);
      }

      .preview-image {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
        cursor: zoom-in;
      }

      .zoom-hint {
        position: absolute;
        bottom: 8px;
        left: 50%;
        transform: translateX(-50%);
        padding: 4px 12px;
        background: rgba(0, 0, 0, 0.6);
        backdrop-filter: blur(10px);
        border-radius: 4px;
        color: white;
        font-size: 12px;
        opacity: 0;
        transition: opacity 0.2s ease;
        z-index: 5;
      }

      .image-area:hover .zoom-hint {
        opacity: 1;
      }

      .clear-button {
        position: absolute;
        top: 8px;
        right: 8px;
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: rgba(0, 0, 0, 0.6);
        backdrop-filter: blur(10px);
        border: none;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        font-size: 18px;
        color: white;
        opacity: 0;
        z-index: 10;
        transition: all 0.15s ease;
      }

      .image-area:hover .clear-button {
        opacity: 1;
      }
      .clear-button:hover {
        background: rgba(0, 0, 0, 0.8);
      }

      .loading-overlay {
        position: absolute;
        inset: 0;
        background: rgba(255, 255, 255, 0.9);
        backdrop-filter: blur(8px);
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 8px;
        z-index: 5;
      }

      .spinner {
        width: 32px;
        height: 32px;
        border: 3px solid rgba(0, 0, 0, 0.08);
        border-top-color: #007aff;
        border-radius: 50%;
        animation: spin 0.7s linear infinite;
      }

      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }

      .drop-zone {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 12px;
        padding: 48px;
        text-align: center;
        width: 100%;
      }

      .drop-icon {
        width: 56px;
        height: 56px;
        border-radius: 14px;
        background: rgba(0, 122, 255, 0.12);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #007aff;
      }

      .drop-text {
        font-size: 15px;
        color: #1d1d1f;
        margin: 0;
      }

      .drop-hint {
        font-size: 14px;
        color: #86868b;
        margin: 0;
      }

      .result-content {
        flex: 1;
        display: flex;
        flex-direction: column;
      }

      .empty-state {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #86868b;
        font-size: 14px;
        text-align: center;
      }

      .result-text {
        font-size: 15px;
        line-height: 1.6;
        color: #1d1d1f;
        margin: 0;
      }

      .detection-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .detection-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px;
        background: rgba(0, 0, 0, 0.04);
        border-radius: 10px;
      }

      .detection-name {
        font-size: 15px;
        color: #1d1d1f;
      }

      .confidence {
        font-size: 14px;
        color: #6e6e73;
      }

      .ocr-text {
        font-family: inherit;
        font-size: 14px;
        line-height: 1.7;
        color: #1d1d1f;
        white-space: pre-wrap;
        word-break: break-word;
        margin: 0;
        overflow-y: auto;
        flex: 1;
      }

      .error-message {
        padding: 12px;
        background: #ffebee;
        color: #c62828;
        border-radius: 8px;
        font-size: 14px;
        border: 1px solid #ffcdd2;
      }

      .action-area {
        padding-top: 16px;
        border-top: 1px solid rgba(0, 0, 0, 0.08);
      }

      .action-button {
        width: 100%;
        padding: 14px 24px;
        font-size: 15px;
        font-weight: 500;
        border: none;
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
      }

      .action-button:hover:not(:disabled) {
        background: #0071e3;
      }

      .action-button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .btn-spinner {
        width: 16px;
        height: 16px;
        border: 2px solid rgba(255, 255, 255, 0.3);
        border-top-color: white;
        border-radius: 50%;
        animation: spin 0.7s linear infinite;
      }

      /* Zoom Modal */
      .zoom-modal {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.9);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
        cursor: pointer;
      }

      .zoom-content {
        position: relative;
        max-width: 90vw;
        max-height: 90vh;
      }

      .zoom-content img {
        max-width: 100%;
        max-height: 90vh;
        object-fit: contain;
      }

      .zoom-close {
        position: absolute;
        top: -40px;
        right: 0;
        width: 32px;
        height: 32px;
        background: rgba(255, 255, 255, 0.2);
        border: none;
        border-radius: 50%;
        color: white;
        font-size: 24px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .zoom-close:hover {
        background: rgba(255, 255, 255, 0.3);
      }
    `,
  ],
})
export class VisionPanelComponent {
  private readonly api = inject(ApiService);
  protected readonly i18n = inject(I18nService);

  activeTask = signal<TaskType>('caption');
  tabStates = signal<Record<TaskType, TabState>>({
    caption: { image: null, file: null, result: null, error: null },
    detect: { image: null, file: null, result: null, error: null },
    ocr: { image: null, file: null, result: null, error: null },
  });
  isLoading = signal(false);
  zoomedImage = signal<string | null>(null);

  taskOptions = computed(() => {
    const t = this.i18n.t().imageUploader;
    return [
      { value: 'caption' as TaskType, label: t.caption },
      { value: 'detect' as TaskType, label: t.detect },
      { value: 'ocr' as TaskType, label: t.ocr },
    ];
  });

  currentState(): TabState {
    return this.tabStates()[this.activeTask()];
  }

  setActiveTask(task: TaskType) {
    this.activeTask.set(task);
  }

  onImageAreaClick() {
    if (!this.currentState().image) {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.onchange = (e) => this.onFileChange(e);
      input.click();
    }
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.processFile(file);
    }
    input.value = '';
  }

  processFile(file: File) {
    if (!file.type.startsWith('image/')) {
      this.updateState({ error: this.i18n.t().imageUploader.selectImageError });
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      const imageData = e.target?.result as string;
      this.updateState({ image: imageData, file, error: null, result: null });
    };
    reader.readAsDataURL(file);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    const droppedFile = event.dataTransfer?.files[0];
    if (droppedFile) {
      this.processFile(droppedFile);
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
  }

  clearImage(event: Event) {
    event.stopPropagation();
    this.updateState({ image: null, file: null, error: null, result: null });
  }

  zoomImage(image: string) {
    this.zoomedImage.set(image);
  }

  closeZoom() {
    this.zoomedImage.set(null);
  }

  submit() {
    const currentFile = this.currentState().file;
    if (!currentFile) return;

    this.isLoading.set(true);
    this.updateState({ error: null, result: null });

    const task = this.activeTask();
    let request: Observable<unknown>;
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
      next: (data: unknown) => {
        this.updateState({ result: data as VisionResult });
      },
      error: (err: unknown) => {
        const msg =
          err instanceof Error ? err.message : this.i18n.t().imageUploader.processingFailed;
        this.updateState({ error: msg });
      },
      complete: () => {
        this.isLoading.set(false);
      },
    });
  }

  private updateState(partial: Partial<TabState>) {
    this.tabStates.update((states) => ({
      ...states,
      [this.activeTask()]: { ...states[this.activeTask()], ...partial },
    }));
  }
}
