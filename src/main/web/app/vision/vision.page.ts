import { Component, signal, inject, ChangeDetectionStrategy, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@core/i18n';
import { SegmentedControlComponent } from '@shared/components/ui/segmented-control/segmented-control.component';
import type { VisionResult } from './vision.model';

type TaskType = 'caption' | 'detect' | 'ocr';

interface TabState {
  image: string | null;
  file: File | null;
  result: VisionResult | null;
  error: string | null;
}

@Component({
  selector: 'app-vision-page',
  imports: [CommonModule, FormsModule, SegmentedControlComponent],
  standalone: true,
  templateUrl: './vision.page.html',
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
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VisionPageComponent {
  private readonly api = inject(ApiService);
  protected readonly i18n = inject(I18nService);

  readonly activeTask = signal<TaskType>('caption');
  readonly tabStates = signal<Record<TaskType, TabState>>({
    caption: { image: null, file: null, result: null, error: null },
    detect: { image: null, file: null, result: null, error: null },
    ocr: { image: null, file: null, result: null, error: null },
  });

  readonly isLoading = signal(false);
  readonly zoomedImage = signal<string | null>(null);

  readonly taskOptions = computed(() => {
    const t = this.i18n.t().imageUploader;
    return [
      { value: 'caption' as TaskType, label: t.caption },
      { value: 'detect' as TaskType, label: t.detect },
      { value: 'ocr' as TaskType, label: t.ocr },
    ];
  });

  readonly currentState = computed<TabState>(() => this.tabStates()[this.activeTask()]);

  setActiveTask(task: TaskType) {
    this.activeTask.set(task);
  }

  onImageAreaClick() {
    if (!this.currentState().image) {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.onchange = e => this.onFileChange(e);
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
        const msg = err instanceof Error
          ? err.message
          : this.i18n.t().imageUploader.processingFailed;
        this.updateState({ error: msg });
      },
      complete: () => {
        this.isLoading.set(false);
      },
    });
  }

  private updateState(partial: Partial<TabState>) {
    this.tabStates.update(states => ({
      ...states,
      [this.activeTask()]: { ...states[this.activeTask()], ...partial },
    }));
  }
}
